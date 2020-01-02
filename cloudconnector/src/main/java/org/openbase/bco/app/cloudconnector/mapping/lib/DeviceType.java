package org.openbase.bco.app.cloudconnector.mapping.lib;

/*-
 * #%L
 * BCO Cloud Connector
 * %%
 * Copyright (C) 2018 - 2020 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

/**
 * Enum mapping device types by Google.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public enum DeviceType {

    AC_UNIT,
    DRYER,
    LIGHT,
    OUTLET,
    SCENE,
    SWITCH,
    THERMOSTAT;

    public static final String REPRESENTATION_PREFIX = "action.devices.types.";

    private final String representation;

    DeviceType() {
        this.representation = REPRESENTATION_PREFIX + this.name();
    }

    public String getRepresentation() {
        return representation;
    }
}
