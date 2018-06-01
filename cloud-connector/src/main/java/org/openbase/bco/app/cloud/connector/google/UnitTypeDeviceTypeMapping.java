package org.openbase.bco.app.cloud.connector.google;

/*-
 * #%L
 * BCO Cloud Connector
 * %%
 * Copyright (C) 2018 openbase.org
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

import org.openbase.jul.exception.NotAvailableException;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

public enum UnitTypeDeviceTypeMapping {

    LIGHT_DEVICE_TYPE_MAPPING(UnitType.LIGHT, DeviceType.LIGHT),
    DIMMABLE_LIGHT_DEVICE_TYPE_MAPPING(UnitType.DIMMABLE_LIGHT, DeviceType.LIGHT),
    COLORABLE_LIGHT_DEVICE_TYPE_MAPPING(UnitType.COLORABLE_LIGHT, DeviceType.LIGHT);

    public static final String POSTFIX = "_DEVICE_TYPE_MAPPING";

    private final UnitType unitType;
    private final DeviceType deviceType;

    UnitTypeDeviceTypeMapping(final UnitType unitType, final DeviceType deviceType) {
        this.unitType = unitType;
        this.deviceType = deviceType;
    }

    public UnitType getUnitType() {
        return unitType;
    }

    public DeviceType getDeviceType() {
        return deviceType;
    }

    public static UnitTypeDeviceTypeMapping getByUnitType(final UnitType unitType) throws NotAvailableException {
        try {
            return UnitTypeDeviceTypeMapping.valueOf(unitType.name() + POSTFIX);
        } catch (IllegalArgumentException ex) {
            throw new NotAvailableException("UnitTypeDeviceTypeMapping for unitType[" + unitType.name() + "]");
        }
    }
}
