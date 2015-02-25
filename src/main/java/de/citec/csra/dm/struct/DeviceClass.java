/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.struct;

import de.citec.jps.core.JPService;
import de.citec.jp.JPDeviceClassDirectory;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author mpohling
 */
public class DeviceClass extends AbstractDeviceStruct<DeviceClass> {

	public enum Category {Other, HomeAutomation, WearableComputing, VisualSensing, VisualDisplay, AcousticSensing, AuditoryDisplay, Infrastructure, CamRGB, CamDepth};
	
	private String productNumber;
	private Category category;
	private final List<String> supportedDataStreams;

	public DeviceClass() {
		this("", "", "", "", Category.Other, new ArrayList<String>());
	}

	public DeviceClass(String id, String name, String description, String productNumber, Category category, List<String> supportedDataStreams) {
		super(id, name, description);
		this.productNumber = productNumber;
		this.category = category;
		this.supportedDataStreams = supportedDataStreams;
	}

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

	public Category getCategory() {
		return category;
	}

	public void setCategory(Category category) {
		this.category = category;
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
		return JPService.getAttribute(JPDeviceClassDirectory.class).getValue();
	}
}
