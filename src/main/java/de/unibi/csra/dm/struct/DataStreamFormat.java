/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.unibi.csra.dm.struct;

/**
 *
 * @author mpohling
 */
public class DataStreamFormat {
	
	private String type;
	private int widht;
	private int height;

	public DataStreamFormat() {
		this("", 0, 0);
	}
	
	public DataStreamFormat(String type, int widht, int height) {
		this.type = type;
		this.widht = widht;
		this.height = height;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getWidht() {
		return widht;
	}

	public void setWidht(int widht) {
		this.widht = widht;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}
	
	public String generateID() {
		return widht+"x"+height+"_"+type;
	}
	
	@Override
	public String toString() {
		return generateID();
	}
}
