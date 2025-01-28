package org.zeith.gson.stream.itf;

import com.google.gson.*;
import lombok.SneakyThrows;
import org.zeith.gson.stream.*;

import java.lang.reflect.*;
import java.util.*;

public class ExportedInterface<T>
{
	private final T instance;
	private final Map<String, Map<Integer, Method>> methods;
	
	public ExportedInterface(boolean requireExportAnnotation,Class<T> interfaceType, T instance)
	{
		if(!interfaceType.isInterface()) throw new UnsupportedOperationException("Can't export non-interface " + interfaceType);
		
		this.instance = instance;
		
		Map<String, Map<Integer, Method>> methods = new HashMap<>();
		for(Method dm : interfaceType.getDeclaredMethods())
		{
			if(requireExportAnnotation && !dm.isAnnotationPresent(Exported.class)) continue;
			methods.computeIfAbsent(dm.getName(), k -> new HashMap<>())
				   .put(dm.getParameterCount(), dm);
		}
		methods.replaceAll((s, methods1) -> Map.copyOf(methods1));
		this.methods = Map.copyOf(methods);
	}
	
	public JsonElement execute(Gson gson, JsonObject payload)
	{
		String method = payload.get("m").getAsString();
		int mp = payload.get("p").getAsInt();
		var mth = methods.getOrDefault(method, Map.of()).get(mp);
		if(mth == null) return encodeError(new NoSuchMethodException(method));
		try
		{
			Object[] argsDeser = new Object[mp];
			JsonArray args = payload.getAsJsonArray("a");
			int i = 0;
			for(Type type : mth.getGenericParameterTypes())
			{
				var res = args.get(i);
				argsDeser[i] = res.isJsonNull() ? null : gson.fromJson(res, type);
				++i;
			}
			
			var res = mth.invoke(instance, argsDeser);
			
			var type = mth.getGenericReturnType();
			if(void.class.equals(type))
				return new JsonObject();
			
			JsonObject resp = new JsonObject();
			resp.add("d", gson.toJsonTree(res, type));
			return resp;
		} catch(Throwable e)
		{
			Throwable realErr = e;
			if(realErr instanceof InvocationTargetException ite)
				realErr = ite.getCause();
			return encodeError(realErr);
		}
	}
	
	public void listen(IJsonChannel pipe)
	{
		listen(pipe.gson(), pipe.writer(), pipe.reader());
	}
	
	@SneakyThrows
	public void listen(Gson gson, IJsonWriter writer, IJsonReader reader)
	{
		while(true)
		{
			var data = reader.read();
			if(data == null) break;
			if(!data.isJsonObject()) continue;
			writer.write(execute(gson, data.getAsJsonObject()));
		}
	}
	
	private static JsonElement encodeError(Throwable err)
	{
		JsonObject o = new JsonObject();
		o.addProperty("e", err.getClass().getName());
		o.addProperty("m", err.getMessage());
		o.add("s", StackTraceElementAdapter.stackToJson(err.getStackTrace()));
		return o;
	}
}