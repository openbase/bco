package org.openbase.bco.registry.device.lib.util;

/*
 * #%L
 * REM DeviceRegistry Library
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
import java.util.ArrayList;
import java.util.List;
import rst.homeautomation.device.DeviceClassType.DeviceClassOrBuilder;
import rst.homeautomation.device.DeviceConfigType.DeviceConfigOrBuilder;
import rst.homeautomation.unit.UnitConfigType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitTemplateConfigType;
import rst.homeautomation.unit.UnitTemplateType;

/**
 * A collection of utils to manipulate or analyse device configs.
 *
 * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public class DeviceConfigUtils {

    /**
     * Check if the given device configuration contains one unit template type more than once.
     *
     * @param deviceConfig
     * @return true if a duplicated unit type is detected.
     */
    public static boolean checkDuplicatedUnitType(final DeviceConfigOrBuilder deviceConfig) {

        List<UnitTemplateType.UnitTemplate.UnitType> unitTypeList = new ArrayList<>();
        for (UnitConfigType.UnitConfig unitConfig : deviceConfig.getUnitConfigList()) {
            if (unitTypeList.contains(unitConfig.getType())) {
                return true;
            }
            unitTypeList.add(unitConfig.getType());
        }
        return false;
    }

    /**
     * Method setups a non defined unit label field or setups the unit label with the device label if the unit is bound to the device.
     *
     * @param unitConfig the unit config to setup the label.
     * @param deviceConfig the device config to lookup device label.
     * @param deviceClass the device class to lookup the unit template.
     * @return true if the unitConfig was modified otherwise false.
     * @throws org.openbase.jul.exception.CouldNotPerformException
     */
    public static boolean setupUnitLabelByDeviceConfig(final UnitConfig.Builder unitConfig, final DeviceConfigOrBuilder deviceConfig, final DeviceClassOrBuilder deviceClass) throws CouldNotPerformException {
        return setupUnitLabelByDeviceConfig(unitConfig, deviceConfig, deviceClass, checkDuplicatedUnitType(deviceConfig));
    }

    /**
     * Method setups a non defined unit label field or setups the unit label with the device label if the unit is bound to the device.
     *
     * @param unitConfig the unit config to setup the label.
     * @param deviceConfig the device config to lookup device label.
     * @param deviceClass the device class to lookup the unit template.
     * @param deviceConfigHasDuplicatedUnitType can be precomputed out of performance reasons.
     * @return true if the unitConfig was modified otherwise false.
     * @throws org.openbase.jul.exception.CouldNotPerformException
     */
    public static boolean setupUnitLabelByDeviceConfig(final UnitConfig.Builder unitConfig, final DeviceConfigOrBuilder deviceConfig, final DeviceClassOrBuilder deviceClass, boolean deviceConfigHasDuplicatedUnitType) throws CouldNotPerformException {
        try {
            if (!unitConfig.hasLabel() || unitConfig.getLabel().isEmpty() || unitConfig.getBoundToSystemUnit()) {
                if (deviceConfigHasDuplicatedUnitType) {
                    if (unitConfig.hasLabel() && !unitConfig.getLabel().isEmpty()) {
                        return false;
                    }

                    if (!unitConfig.hasUnitTemplateConfigId()) {
                        throw new NotAvailableException("unitconfig.unittemplateconfigid");
                    }

                    for (UnitTemplateConfigType.UnitTemplateConfig unitTemplateConfig : deviceClass.getUnitTemplateConfigList()) {
                        if (unitTemplateConfig.getId().equals(unitConfig.getUnitTemplateConfigId())) {
                            if(unitTemplateConfig.getLabel().isEmpty()) {
                                throw new NotAvailableException("unitTemplateConfig.label");
                            }
                            unitConfig.setLabel(deviceConfig.getLabel() + "_" + unitTemplateConfig.getLabel());
                            return true;
                        }
                    }
                    throw new CouldNotPerformException("DeviceClass[" + deviceClass.getId() + "] does not contain UnitTemplateConfig[" + unitConfig.getUnitTemplateConfigId() + "]!");
                } else {

                    if (!deviceConfig.hasLabel()) {
                        throw new NotAvailableException("deviceconfig.label");
                    }

                    if (!unitConfig.getLabel().equals(deviceConfig.getLabel())) {
                        unitConfig.setLabel(deviceConfig.getLabel());
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not setup UnitConfig[" + unitConfig.getId() + "] by DeviceConfig[" + deviceConfig + "]!", ex);
        }
    }
}
