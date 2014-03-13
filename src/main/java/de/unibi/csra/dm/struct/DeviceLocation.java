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
public class DeviceLocation {

	private String description;
	private final double[] position;
	private final double[] rotation;

	public DeviceLocation() {
		this("", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
	}

	public DeviceLocation(String description, double x, double y, double z, double alpha, double beta, double gamma) {
		this.description = description;
		position = new double[3];
		position[0] = x;
		position[1] = y;
		position[2] = z;
		rotation = new double[3];
		rotation[0] = alpha;
		rotation[1] = beta;
		rotation[2] = gamma;
	}

	public DeviceLocation(String description, double[] position, double[] rotation) {
		this.description = description;
		this.position = position;
		this.rotation = rotation;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public double[] getPosition() {
		return position;
	}

	public void setPosition(double[] position) {
		this.position[0] = position[0];
		this.position[1] = position[1];
		this.position[2] = position[2];
	}

	public double[] getRotation() {
		return rotation;
	}

	public void setRotation(double[] rotation) {
		this.rotation[0] = rotation[0];
		this.rotation[1] = rotation[1];
		this.rotation[2] = rotation[2];
	}

	@Override
	public String toString() {
		return getClass().getSimpleName()+"["+position[0]+"x"+position[1]+"x"+position[2]+"]";
	}
}
