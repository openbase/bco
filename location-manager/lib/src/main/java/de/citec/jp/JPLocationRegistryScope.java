/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.jp;

import org.dc.jul.extension.rsb.scope.jp.JPScope;
import rsb.Scope;

/**
 *
 * @author mpohling
 */
public class JPLocationRegistryScope extends JPScope {
    
	public final static String[] COMMAND_IDENTIFIERS = {"--location-registry-scope"};

	public JPLocationRegistryScope() {
		super(COMMAND_IDENTIFIERS);
	}
    
    @Override
    protected Scope getPropertyDefaultValue() {
        return new Scope("/locationmanager/registry");
    }
    
    @Override
	public String getDescription() {
		return "Setup the location registry scope which is used for the rsb communication.";
    }
}