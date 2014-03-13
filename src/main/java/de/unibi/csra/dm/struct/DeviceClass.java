/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.csra.dm.struct;

import de.unibi.agai.clparser.CLParser;
import de.unibi.agai.clparser.command.CLDeviceClassDirectory;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author mpohling
 */
public class DeviceClass extends AbstractDeviceStruct<DeviceClass> {

//	private String id;
//	private String name;
//	private String description;
	private String productNumber;
	private final List<String> supportedDataStreams;

	public DeviceClass() {
		this("", "", "", "", new ArrayList<String>());
	}

	public DeviceClass(String id, String name, String description, String productNumber, List<String> supportedDataStreams) {
//		this.id = id;
//		this.name = name;
//		this.description = description;
		super(id, name, description);
		this.productNumber = productNumber;
		this.supportedDataStreams = supportedDataStreams;
	}
//
//	public String getId() {
//		return id;
//	}
//
//	public String getName() {
//		return name;
//	}
//
//	public void setName(String name) {
//		this.name = name;
//	}
//
//	public String getDescription() {
//		return description;
//	}
//
//	public void setDescription(String description) {
//		this.description = description;
//	}

	public String getProductNumber() {
		return productNumber;
	}

	public void setProductNumber(String productNumber) {
		this.productNumber = productNumber;
	}
	
	public void addSupportedDataStream(final DataStream dataStream) {
		supportedDataStreams.add(dataStream.getId());
	}
	
	public void removeSupportedDataStream(final DataStream dataStream) {
		supportedDataStreams.remove(dataStream.getId());
	}

	public synchronized void setSupportedDataStreams(final List<String> supportedDataStreams) {
		this.supportedDataStreams.clear();
		this.supportedDataStreams.addAll(supportedDataStreams);
	}

	public List<String> getSupportedDataStreams() {
		return Collections.unmodifiableList(supportedDataStreams);
	}
	
	@Override
	public File getParentDirectory() {
		return CLParser.getAttribute(CLDeviceClassDirectory.class).getValue();
	}
}
