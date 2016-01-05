/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.lib.data;

import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.extension.rsb.scope.ScopeProvider;
import org.dc.jul.extension.rsb.scope.ScopeTransformer;
import rsb.Scope;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author Divine Threepwood
 */
public class Location implements ScopeProvider {

    private final LocationConfig config;

	public Location(final LocationConfig config) {
        this.config = config;
	}

	public String getLabel() {
		return config.getLabel();
	}

	public final boolean isRoot() {
		return config.getRoot();
	}

	@Override
	public Scope getScope() throws CouldNotPerformException{
		return ScopeTransformer.transform(config.getScope());
	}

    public LocationConfig getConfig() {
        return config;
    }

	@Override
	public String toString() {
        try {
            return getClass().getSimpleName()+"["+getScope()+"]";
        } catch (CouldNotPerformException ex) {
            return getClass().getSimpleName()+"[?]";
        }
	}
}
