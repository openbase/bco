/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.csra.dm.struct;

import de.unibi.agai.clparser.CLParser;
import de.unibi.agai.clparser.command.CLDeviceInstanceDirectory;
import java.io.File;

/**
 *
 * @author mpohling
 */
public class DeviceInstance extends AbstractDeviceStruct<DeviceInstance> {

	public enum DeviceStatus {

		Available, Ordered, Lost, Borrowed, Installed, Unknown
	}

	private String deviceClass;
	private String serialNumber;
	private final DeviceLocation location;
	private DeviceStatus status;

	public DeviceInstance(String id, String name, String description, String deviceClass, String serialNumber, DeviceLocation location, DeviceStatus status) {
		super(id, name, description);
		this.deviceClass = deviceClass;
		this.serialNumber = serialNumber;
		this.location = location;
		this.status = status;
	}

	public DeviceInstance() {
		this.deviceClass = "";
		this.serialNumber = "";
		this.location = new DeviceLocation();
		this.status = DeviceStatus.Unknown;
	}
	
	public String getDeviceClass() {
		return deviceClass;
	}

	public void setDeviceClass(String deviceClass) {
		this.deviceClass = deviceClass;
	}

	public String getSerialNumber() {
		return serialNumber;
	}

	public void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
	}

	public DeviceLocation getLocation() {
		return location;
	}

	public DeviceStatus getStatus() {
		return status;
	}

	public void setStatus(DeviceStatus status) {
		this.status = status;
	}
	
	@Override
	public String generateName() {
		Integer highestID = 0;
		String fileName = "";
		String fileNamePrefix = deviceClass + "_";

		for (File file : CLParser.getAttribute(CLDeviceInstanceDirectory.class).getValue().listFiles()) {
			try {
				fileName = file.getName();
				if (fileName.startsWith(fileNamePrefix)) {
					fileName = fileName.replaceFirst(fileNamePrefix, "");
					highestID = Math.max(highestID, Integer.parseInt(fileName));
				}
			} catch (NumberFormatException ex) {
				System.err.println("Could process filename[" + file.getName() + "! Could not parse Integer[" + fileName + "]");
			}
		}
		return fileNamePrefix + ++highestID;
	}

	@Override
	public File getParentDirectory() {
		return CLParser.getAttribute(CLDeviceInstanceDirectory.class).getValue();
	}
}
