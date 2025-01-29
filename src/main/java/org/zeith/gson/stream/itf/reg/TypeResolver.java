package org.zeith.gson.stream.itf.reg;

public interface TypeResolver
{
	TypeResolver FOR_NAME = Class::forName;
	
	Class<?> loadClass(String name)
			throws ClassNotFoundException;
	
	static TypeResolver fromClassLoader(ClassLoader loader)
	{
		return loader::loadClass;
	}
}