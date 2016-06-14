package org.openbase.bco.dal.lib.data;

/*
 * #%L
 * DAL Library
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rst.iface.ScopeProvider;
import rst.rsb.ScopeType;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author Divine Threepwood
 */
@Deprecated
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
	public ScopeType.Scope getScope() throws NotAvailableException{
		return config.getScope();
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
