/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.app.lib.jp;

import org.dc.jul.extension.rsb.scope.jp.JPScope;
import rsb.Scope;

/**
 *
 * @author mpohling
 */
public class JPAppRegistryScope extends JPScope {
    
	public final static String[] COMMAND_IDENTIFIERS = {"--app-registry-scope"};

	public JPAppRegistryScope() {
		super(COMMAND_IDENTIFIERS);
	}

    @Override
    protected Scope getPropertyDefaultValue() {
        return new Scope("/appmanager/registry");
    }
    
    @Override
	public String getDescription() {
		return "Setup the app registry scope which is used for the rsb communication.";
    }
}