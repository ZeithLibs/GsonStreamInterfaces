package org.zeith.gson.stream.itf;

import com.google.gson.*;
import org.zeith.gson.stream.*;

import java.lang.reflect.*;
import java.nio.channels.ClosedChannelException;

public interface ImportedInterface
{
	static <T> T imported(Class<T> type, IJsonChannel pipe)
	{
		return imported(type, pipe.gson(), pipe.writer(), pipe.reader());
	}
	
	static <T> T imported(Class<T> type, Gson gson, IJsonWriter output, IJsonReader input)
	{
		//noinspection unchecked
		return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[] {type, ImportedInterface.class}, (proxy, method, args) ->
				{
					JsonObject o = new JsonObject();
					o.addProperty("m", method.getName());
					o.addProperty("p", method.getParameterCount());
					var pts = method.getGenericParameterTypes();
					if(args != null)
					{
						JsonArray as = new JsonArray();
						for(int i = 0; i < args.length; i++)
							as.add(gson.toJsonTree(args[i], pts[i]));
						o.add("a", as);
					}
					output.write(o);
					
					var resp = input.read();
					if(resp == null) throw new RemoteSpoofException(ClosedChannelException.class.getName(), "Input closed.");
					o = resp.getAsJsonObject();
					
					var err = decodeError(o);
					if(err != null) throw err;
					
					return gson.fromJson(o.get("d"), method.getGenericReturnType());
				}
		);
	}
	
	static RuntimeException decodeError(JsonObject o)
	{
		if(!o.has("e")) return null;
		
		String errorClass = o.get("e").getAsString();
		var msgRaw = o.get("m");
		String message = msgRaw.isJsonNull() ? null : msgRaw.getAsString();
		
		Throwable causedBy = null;
		
		try
		{
			Class<? extends Throwable> cl = ImportedInterface.class.getClassLoader().loadClass(errorClass).asSubclass(Throwable.class);
			for(Constructor<?> ctr : cl.getDeclaredConstructors())
			{
				if(ctr.getParameterCount() == 1 && ctr.getParameterTypes()[0].equals(String.class))
				{
					causedBy = (Throwable) ctr.newInstance(message);
				}
			}
		} catch(Throwable ignored)
		{
		}
		
		if(causedBy == null) causedBy = new RemoteSpoofException(errorClass, message);
		
		causedBy.setStackTrace(StackTraceElementAdapter.stackFromJson(o.getAsJsonArray("s")));
		
		return new RemoteException("Exception on remote", causedBy);
	}
}