package org.zeith.gson.stream.itf.reg;

public record Type(String c)
{
	public static Type of(Class<?> type)
	{
		return new Type(type.getName());
	}
	
	public Class<?> resolve()
			throws ClassNotFoundException
	{
		return Class.forName(c);
	}
	
	public Class<?> resolve(ClassLoader loader)
			throws ClassNotFoundException
	{
		return loader.loadClass(c);
	}
}