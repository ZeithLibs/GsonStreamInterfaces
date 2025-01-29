package org.zeith.gson.stream.itf.reg;

import lombok.*;
import org.zeith.gson.stream.*;
import org.zeith.gson.stream.itf.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class InterfaceRegistry
		implements AutoCloseable
{
	static final String SERVER_PREFIX = "s_";
	static final String CLIENT_PREFIX = "c_";
	static final String IMPORT_CHANNEL = "@{import}";
	static final String EXPORT_CHANNEL = "@{export}";
	static final Set<String> UNCLOSEABLE_CHANNELS = Set.of(IMPORT_CHANNEL, EXPORT_CHANNEL);
	
	private final boolean server;
	private final String exportPrefix, importPrefix;
	private final Executor exportListener;
	private final MultiJsonChannel channel;
	
	private final Map<String, ExportedInstance> allExports = new HashMap<>();
	private final Map<String, ImportedInstance> allImports = new HashMap<>();
	
	private final ExportRegistryListener remoteExports;
	private final ImportRegistryListener remoteImports;
	
	private final boolean requireExportAnnotation;
	
	private final TypeResolver loader;
	
	public InterfaceRegistry(TypeResolver loader, boolean server, boolean requireExportAnnotation, Executor exportListener, MultiJsonChannel channel)
	{
		this.loader = loader;
		this.server = server;
		this.requireExportAnnotation = requireExportAnnotation;
		this.exportListener = exportListener;
		this.channel = channel;
		this.exportPrefix = server ? SERVER_PREFIX : CLIENT_PREFIX;
		this.importPrefix = server ? CLIENT_PREFIX : SERVER_PREFIX;
		
		exported(EXPORT_CHANNEL, ExportRegistryListener.class, new ExportListener());
		exported(IMPORT_CHANNEL, ImportRegistryListener.class, new ImportListener());
		
		remoteExports = imported(EXPORT_CHANNEL, ExportRegistryListener.class);
		remoteImports = imported(IMPORT_CHANNEL, ImportRegistryListener.class);
	}
	
	public Map<String, Type> getExported()
	{
		return remoteExports != null ? remoteExports.getExported() : Map.of();
	}
	
	@Synchronized
	public <T> void exported(String guid, Class<T> itf, T instance)
	{
		if(!itf.isInterface())
			throw new IllegalArgumentException(itf + " is not an interface.");
		if(allExports.containsKey(guid))
			throw new IllegalArgumentException("Guid {" + guid + "} is already exported.");
		var subch = channel.registerChannel(exportPrefix + guid);
		ExportedInterface<T> eitf = new ExportedInterface<>(requireExportAnnotation, itf, instance);
		var fut = CompletableFuture.runAsync(() -> eitf.listen(subch), exportListener);
		allExports.put(guid, new ExportedInstance(itf, eitf, fut, subch));
		if(remoteImports != null) remoteImports.onRegistered(guid, Type.of(itf));
	}
	
	@Synchronized
	public <T> T imported(String guid, Class<T> itf)
	{
		if(guid == null || guid.isBlank() || itf == null)
			return null;
		if(!itf.isInterface())
			throw new IllegalArgumentException(itf + " is not an interface.");
		return itf.cast(allImports.computeIfAbsent(guid, key ->
				{
					var subch = channel.registerChannel(importPrefix + guid);
					var imported = (ImportedInterface) ImportedInterface.imported(itf, subch);
					return new ImportedInstance(imported, subch);
				}
		).itf());
	}
	
	@SneakyThrows
	@Synchronized
	public void unexport(String guid)
	{
		if(UNCLOSEABLE_CHANNELS.contains(guid))
			throw new IllegalArgumentException("Unable to unexport guid {" + guid + "}");
		var exp = allExports.remove(guid);
		if(exp == null) return;
		exp.close();
		if(remoteImports != null) remoteImports.onDeregistered(guid);
	}
	
	@SneakyThrows
	@Synchronized
	public void unimport(String guid)
	{
		if(UNCLOSEABLE_CHANNELS.contains(guid))
			throw new IllegalArgumentException("Unable to unimport guid {" + guid + "}");
		var exp = allImports.remove(guid);
		if(exp == null) return;
		exp.close();
	}
	
	@Override
	@Synchronized
	public void close()
			throws IOException
	{
		for(var exported : allExports.values()) exported.close();
		allExports.clear();
		
		for(var imported : allImports.values()) imported.close();
		allImports.clear();
	}
	
	private class ImportListener
			implements ImportRegistryListener
	
	{
		@Override
		public void onRegistered(String guid, Type type)
		{
			try
			{
				imported(guid, type.resolve(loader));
			} catch(ClassNotFoundException e)
			{
				throw new RuntimeException(e);
			}
		}
		
		@Override
		public void onDeregistered(String guid)
		{
			unimport(guid);
		}
	}
	
	private class ExportListener
			implements ExportRegistryListener
	{
		@Override
		public Map<String, Type> getExported()
		{
			return allExports
					.entrySet()
					.stream()
					.collect(Collectors.toMap(Map.Entry::getKey, e -> Type.of(e.getValue().type())));
		}
	}
	
	private record ExportedInstance(Class<?> type, ExportedInterface<?> itf, CompletableFuture<?> listenFuture, IJsonChannel channel)
	{
		void close()
				throws IOException
		{
			channel.close();
			listenFuture.cancel(true);
		}
	}
	
	private record ImportedInstance(ImportedInterface itf, IJsonChannel channel)
	{
		void close()
				throws IOException
		{
			channel.close();
		}
	}
}