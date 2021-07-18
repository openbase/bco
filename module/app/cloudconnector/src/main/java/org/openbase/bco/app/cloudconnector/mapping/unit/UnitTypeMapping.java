package org.openbase.bco.app.cloudconnector.mapping.unit;

/*-
 * #%L
 * BCO Cloud Connector
 * %%
 * Copyright (C) 2018 - 2021 openbase.org
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

import org.openbase.bco.app.cloudconnector.mapping.lib.DeviceType;
import org.openbase.bco.app.cloudconnector.mapping.lib.Trait;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public enum UnitTypeMapping {

    AGENT_MAPPING(UnitType.AGENT, DeviceType.SCENE,
            map(ServiceType.ACTIVATION_STATE_SERVICE, Trait.SCENE),
            map(ServiceType.EMPHASIS_STATE_SERVICE, Trait.MODES)),
    APP_MAPPING(UnitType.APP, DeviceType.SCENE,
            map(ServiceType.ACTIVATION_STATE_SERVICE, Trait.SCENE)),
    BATTERY_MAPPING(UnitType.BATTERY, DeviceType.DRYER,
            map(ServiceType.BATTERY_STATE_SERVICE, Trait.MODES)),
    BUTTON_MAPPING(UnitType.BUTTON, DeviceType.OUTLET,
            map(ServiceType.BUTTON_STATE_SERVICE, Trait.MODES)),
    COLORABLE_LIGHT_MAPPING(UnitType.COLORABLE_LIGHT, DeviceType.LIGHT,
            map(ServiceType.POWER_STATE_SERVICE, Trait.ON_OFF),
            map(ServiceType.BRIGHTNESS_STATE_SERVICE, Trait.BRIGHTNESS),
            map(ServiceType.COLOR_STATE_SERVICE, Trait.COLOR_SETTING)),
    DIMMABLE_LIGHT_MAPPING(UnitType.DIMMABLE_LIGHT, DeviceType.LIGHT,
            map(ServiceType.POWER_STATE_SERVICE, Trait.ON_OFF),
            map(ServiceType.BRIGHTNESS_STATE_SERVICE, Trait.BRIGHTNESS)),
    DIMMER_MAPPING(UnitType.DIMMER, DeviceType.LIGHT,
            map(ServiceType.POWER_STATE_SERVICE, Trait.ON_OFF),
            map(ServiceType.BRIGHTNESS_STATE_SERVICE, Trait.BRIGHTNESS)),
    LIGHT_MAPPING(UnitType.LIGHT, DeviceType.LIGHT,
            map(ServiceType.POWER_STATE_SERVICE, Trait.ON_OFF)),
    MOTION_DETECTOR_MAPPING(UnitType.MOTION_DETECTOR, DeviceType.OUTLET,
            map(ServiceType.MOTION_STATE_SERVICE, Trait.TOGGLES)),
    POWER_CONSUMPTION_SENSOR_MAPPING(UnitType.POWER_CONSUMPTION_SENSOR, DeviceType.OUTLET,
            map(ServiceType.POWER_CONSUMPTION_STATE_SERVICE, Trait.MODES)),
    POWER_SWITCH_MAPPING(UnitType.POWER_SWITCH, DeviceType.SWITCH,
            map(ServiceType.POWER_STATE_SERVICE, Trait.ON_OFF)),
    ROLLER_SHUTTER_MAPPING(UnitType.ROLLER_SHUTTER, DeviceType.OUTLET,
            map(ServiceType.BLIND_STATE_SERVICE, Trait.MODES)),
    SCENE_MAPPING(UnitType.SCENE, DeviceType.SCENE,
            map(ServiceType.ACTIVATION_STATE_SERVICE, Trait.SCENE)),
    TEMPERATURE_CONTROLLER_MAPPING(UnitType.TEMPERATURE_CONTROLLER, DeviceType.THERMOSTAT,
            map(ServiceType.TARGET_TEMPERATURE_STATE_SERVICE, Trait.TEMPERATURE_SETTING));

    public static final String POSTFIX = "_MAPPING";

    private final UnitType unitType;
    private final DeviceType deviceType;
    private final Map<Trait, ServiceType> traitServiceTypeMap;

    UnitTypeMapping(final UnitType unitType, final DeviceType deviceType, final TraitServiceTypeMapping... mappings) {
        this.unitType = unitType;
        this.deviceType = deviceType;
        this.traitServiceTypeMap = new HashMap<>();
        for (final TraitServiceTypeMapping mapping : mappings) {
            traitServiceTypeMap.put(mapping.getTrait(), mapping.getServiceType());
        }
    }

    public UnitType getUnitType() {
        return unitType;
    }

    public DeviceType getDeviceType() {
        return deviceType;
    }

    public Set<Trait> getTraitSet() {
        return traitServiceTypeMap.keySet();
    }

    public ServiceType getServiceType(final Trait trait) {
        return traitServiceTypeMap.get(trait);
    }

    public static UnitTypeMapping getByUnitType(final UnitType unitType) throws NotAvailableException {
        switch (unitType) {
            case MOTION_DETECTOR:
            case BUTTON:
            case POWER_CONSUMPTION_SENSOR:
                throw new NotAvailableException(unitType.name() + " ignored for testing");
        }
        try {
            return UnitTypeMapping.valueOf(unitType.name() + POSTFIX);
        } catch (IllegalArgumentException ex) {
            throw new NotAvailableException("UnitTypeMapping for unitType[" + unitType.name() + "]");
        }
    }

    private static TraitServiceTypeMapping map(final ServiceType serviceType, final Trait trait) {
        return new TraitServiceTypeMapping(serviceType, trait);
    }

    private static class TraitServiceTypeMapping {
        private final ServiceType serviceType;
        private final Trait trait;

        public TraitServiceTypeMapping(final ServiceType serviceType, final Trait trait) {
            this.serviceType = serviceType;
            this.trait = trait;
        }

        public ServiceType getServiceType() {
            return serviceType;
        }

        public Trait getTrait() {
            return trait;
        }
    }
}
