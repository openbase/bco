/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.csra.dm.struct;

import de.unibi.agai.clparser.CLParser;
import de.unibi.agai.clparser.command.CLGlobalConfigDirectory;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author mpohling
 */
public class GlobalConfig extends AbstractDeviceStruct<GlobalConfig> {

	private final Map<String, String> instanceConfigMap;

	public GlobalConfig() {
		this("", "", "", new HashMap<String, String>());
	}

	public GlobalConfig(String id, String name, String description, Map<String, String> instanceConfigMap) {
		super(id, name, description);
		this.instanceConfigMap = instanceConfigMap;
	}

	public void setInstanceConfigMap(final Map<String, String> instanceConfigMap) {
		this.instanceConfigMap.clear();
		this.instanceConfigMap.putAll(instanceConfigMap);
	}

	public Map<String, String> getInstanceConfigMap() {
		return new HashMap<>(instanceConfigMap);
	}
	
	public void addInstanceConfig(DeviceInstance instance, DeviceConfig config) {
		this.instanceConfigMap.put(instance.getId(), config.getId());
	}

	public void removeInstanceConfig(DeviceInstance instance, DeviceConfig config) throws Exception {
		if (!instanceConfigMap.get(instance.getId()).equals(config.getId())) {
			throw new Exception("No match found!");
		}
		instanceConfigMap.remove(instance.getId());
	}

	@Override
	public File getParentDirectory() {
		return CLParser.getAttribute(CLGlobalConfigDirectory.class).getValue();
	}
}
