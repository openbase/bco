/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.csra.dm.struct;

import de.citec.jps.core.JPService;
import de.unibi.agai.clparser.command.CLDataStreamDirectory;
import static de.unibi.csra.dm.struct.DataStream.RST_Type.unknown;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author mpohling
 */
public class DataStream extends AbstractDeviceStruct<DataStream> {

	public enum RST_Type {

		rst_vision_Image("rst.vision.Image"),
		rst_vision_Image_Depth("rst.vision.Image.Depth"),
		unknown("unknown");

		private final String id;

		private RST_Type(String id) {
			this.id = id;
		}
	}

	private RST_Type rst_type;
	private final Map<String, String> metaAttributeMap;
	private int frequency, dataSize;

	public DataStream() {
		this("", "", "", 0, unknown, 0, new HashMap<String, String>());
	}

	public DataStream(String id, String name, String description, int dataSize, RST_Type rst_type, int frequency, Map<String, String> metaAttributeMap) {
		super(id, name, description);
		this.dataSize = dataSize;
		this.rst_type = rst_type;
		this.frequency = frequency;
		this.metaAttributeMap = metaAttributeMap;
	}

	public static String generateName(RST_Type type, Integer dataSize, Integer frequency) {
		return type.name() + "_" + dataSize + "_" + frequency;
	}

	@Override
	public String generateName() {
		return generateName(rst_type, dataSize, frequency);
	}

	public int getDataSize() {
		return dataSize;
	}

	public void setDataSize(int dataSize) {
		this.dataSize = dataSize;
	}

	public RST_Type getRst_type() {
		return rst_type;
	}

	public void setRst_type(RST_Type rst_type) {
		this.rst_type = rst_type;
	}

	public int getFrequency() {
		return frequency;
	}

	public void setFrequency(int frequency) {
		this.frequency = frequency;
	}

	public Map getMetaAttributeMap() {
		return new HashMap(metaAttributeMap);
	}

	public void setMetaAttributeMap(final Map<String, String> metaAttributeMap) {
		this.metaAttributeMap.clear();
		this.metaAttributeMap.putAll(metaAttributeMap);
	}

	@Override
	public File getParentDirectory() {
		return JPService.getAttribute(CLDataStreamDirectory.class).getValue();
	}
}
