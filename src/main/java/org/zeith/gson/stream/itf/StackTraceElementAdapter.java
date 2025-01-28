package org.zeith.gson.stream.itf;

import com.google.gson.*;

public class StackTraceElementAdapter
{
	public static JsonArray stackToJson(StackTraceElement[] element)
	{
		JsonArray arr = new JsonArray();
		for(StackTraceElement e : element)
			arr.add(toJson(e));
		return arr;
	}
	
	public static StackTraceElement[] stackFromJson(JsonArray arr)
	{
		StackTraceElement[] elements = new StackTraceElement[arr.size()];
		for(int i = 0; i < elements.length; i++)
			elements[i] = fromJson(arr.get(i).getAsJsonArray());
		return elements;
	}
	
	public static JsonArray toJson(StackTraceElement element)
	{
		var arr = new JsonArray();
		arr.add(element.getClassLoaderName());
		arr.add(element.getModuleName());
		arr.add(element.getModuleVersion());
		arr.add(element.getClassName());
		arr.add(element.getMethodName());
		arr.add(element.getFileName());
		arr.add(element.getLineNumber());
		return arr;
	}
	
	public static StackTraceElement fromJson(JsonArray arr)
	{
		return new StackTraceElement(
				gns(arr, 0),
				gns(arr, 1),
				gns(arr, 2),
				arr.get(3).getAsString(),
				arr.get(4).getAsString(),
				gns(arr, 5),
				arr.get(6).getAsInt()
		);
	}
	
	private static String gns(JsonArray array, int elem)
	{
		var e = array.get(elem);
		return e.isJsonNull() ? null : e.getAsString();
	}
}