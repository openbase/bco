/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.lib.data;

/*
 * #%L
 * DAL Library
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

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
