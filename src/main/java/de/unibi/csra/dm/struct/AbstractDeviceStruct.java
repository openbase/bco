/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.csra.dm.struct;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.citec.jps.core.JPService;
import de.unibi.agai.clparser.command.JPGlobalConfigDirectory;
import de.unibi.csra.dm.tools.Manageable;
import java.io.File;

/**
 *
 * @author mpohling
 * @param <S>
 */
public abstract class AbstractDeviceStruct<S extends AbstractDeviceStruct> implements Manageable<S> {

	@JsonIgnore
	private String id;
	private String name;
	private String description;

	public AbstractDeviceStruct(String id, String name, String description) {
		this.id = id;
		this.name = name;
		this.description = description;
	}

	public AbstractDeviceStruct() {
		this.id = "";
		this.name = "";
		this.description = "";
	}
	
	@Override
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	
	@Override
	public void updateID() {
		if(id.isEmpty()) {
			id = name;
		}
	}
	
	public File getFile() {
		return new File(getParentDirectory(), getId());
	}
	
	public abstract File getParentDirectory();
	
	@Override
	public String generateName() {
		return "";
	}
}
