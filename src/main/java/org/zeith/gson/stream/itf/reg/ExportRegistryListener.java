package org.zeith.gson.stream.itf.reg;

import org.zeith.gson.stream.itf.Exported;

import java.util.Map;

public interface ExportRegistryListener
{
	@Exported
	Map<String, Type> getExported();
}