package org.zeith.gson.stream.itf.reg;

import org.zeith.gson.stream.itf.Exported;

public interface ImportRegistryListener
{
	@Exported
	void onRegistered(String guid, Type type);
	
	@Exported
	void onDeregistered(String guid);
}