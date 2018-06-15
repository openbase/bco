package org.openbase.bco.app.cloud.connector.mapping.unit;

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

import org.openbase.bco.app.cloud.connector.mapping.lib.DeviceType;
import org.openbase.bco.app.cloud.connector.mapping.lib.Trait;
import org.openbase.jul.exception.NotAvailableException;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public enum UnitTypeMapping {

    AGENT_MAPPING(UnitType.AGENT, DeviceType.SCENE, Trait.SCENE),
    APP_MAPPING(UnitType.APP, DeviceType.SCENE, Trait.SCENE),
    COLORABLE_LIGHT_MAPPING(UnitType.COLORABLE_LIGHT, DeviceType.LIGHT, Trait.ON_OFF, Trait.BRIGHTNESS, Trait.COLOR_SPECTRUM, Trait.COLOR_TEMPERATURE),
    DIMMABLE_LIGHT_MAPPING(UnitType.DIMMABLE_LIGHT, DeviceType.LIGHT, Trait.ON_OFF, Trait.BRIGHTNESS),
    DIMMER_MAPPING(UnitType.DIMMER, DeviceType.LIGHT, Trait.ON_OFF, Trait.BRIGHTNESS),
    LIGHT_MAPPING(UnitType.LIGHT, DeviceType.LIGHT, Trait.ON_OFF),
    POWER_SWITCH_MAPPING(UnitType.POWER_SWITCH, DeviceType.SWITCH, Trait.ON_OFF),
    SCENE_MAPPING(UnitType.SCENE, DeviceType.SCENE, Trait.SCENE),
    TEMPERATURE_CONTORLLER_MAPPING(UnitType.TEMPERATURE_CONTROLLER, DeviceType.THERMOSTAT, Trait.TEMPERATURE_SETTING);

    public static final String POSTFIX = "_MAPPING";

    private final UnitType unitType;
    private final DeviceType deviceType;
    private final Set<Trait> traitSet;

    UnitTypeMapping(final UnitType unitType, final DeviceType deviceType, final Trait... traits) {
        this.unitType = unitType;
        this.deviceType = deviceType;
        this.traitSet = new HashSet<>(Arrays.asList(traits));
    }

    public UnitType getUnitType() {
        return unitType;
    }

    public DeviceType getDeviceType() {
        return deviceType;
    }

    public Set<Trait> getTraitSet() {
        return traitSet;
    }

    public static UnitTypeMapping getByUnitType(final UnitType unitType) throws NotAvailableException {
        try {
            return UnitTypeMapping.valueOf(unitType.name() + POSTFIX);
        } catch (IllegalArgumentException ex) {
            throw new NotAvailableException("UnitTypeMapping for unitType[" + unitType.name() + "]");
        }
    }
}
