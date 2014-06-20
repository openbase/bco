/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.citec.csra.dm.struct;

import de.citec.jps.core.JPService;
import de.citec.jps.properties.JPDeviceConfigDirectory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author mpohling
 */
public class DeviceConfig extends AbstractDeviceStruct<DeviceConfig> {
	private String deviceClass;
	private final List<String> dataStreams;

	public DeviceConfig() {
		this("", "", "", "", new ArrayList());
	}
	
	public DeviceConfig(final String id, final String name, final String description, final String deviceClass, final List<String> dataStreams) {
		super(id, name, description);
		this.deviceClass = deviceClass;
		this.dataStreams = dataStreams;
	}
	
	public String getDeviceClass() {
		return deviceClass;
	}

	public void setDeviceClass(final String deviceClass) {
		this.deviceClass = deviceClass;
	}

	public void addDataStream(final DataStream dataStream) {
		this.dataStreams.add(dataStream.toString());
	}
	
	public void removeDataStream(final DataStream dataStream) {
		this.dataStreams.remove(dataStream.toString());
	}
	
	public synchronized void setDataStreams(final List<String> dataStreams) {
		this.dataStreams.clear();
		this.dataStreams.addAll(dataStreams);
	}

	public List<String> getDataStreams() {
		return new ArrayList(dataStreams);
	}
	
	@Override
	public String generateName() {
		return deviceClass + "_";
	}
	
	@Override
	public File getParentDirectory() {
		return JPService.getAttribute(JPDeviceConfigDirectory.class).getValue();
	}
}
