/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.unibi.csra.dm.tools;

/**
 *
 * @author mpohling
 * @param <S>
 */
public interface Manageable<S extends Manageable>  {
	public String getId();
	public String generateName();
	public void updateID();
}
