package org.openbase.bco.dal.lib.layer.unit;

/*
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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

import org.openbase.bco.dal.lib.layer.service.operation.BrightnessStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.ColorStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.PowerStateOperationService;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.type.processing.MetaConfigPool;
import org.openbase.jul.extension.type.processing.MetaConfigVariableProvider;
import org.openbase.jul.extension.type.processing.ScopeProcessor;
import org.openbase.type.domotic.service.ServiceConfigType;
import org.openbase.type.domotic.service.ServiceConfigType.ServiceConfig;
import org.openbase.type.domotic.service.ServiceTemplateConfigType;
import org.openbase.type.domotic.service.ServiceTemplateConfigType.ServiceTemplateConfig;
import org.openbase.type.domotic.service.ServiceTemplateType;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateConfigType;
import org.openbase.type.domotic.unit.UnitTemplateConfigType.UnitTemplateConfig;
import org.openbase.type.domotic.unit.dal.ColorableLightDataType.ColorableLightData;
import org.openbase.type.domotic.unit.device.DeviceClassType;
import org.openbase.type.domotic.unit.device.DeviceClassType.DeviceClass;
import org.openbase.type.vision.ColorType;
import org.openbase.type.vision.ColorType.Color;
import org.openbase.type.vision.HSBColorType.HSBColor;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;

import static org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.COLOR_STATE_SERVICE;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface ColorableLight extends Unit<ColorableLightData>, ColorStateOperationService, BrightnessStateOperationService, PowerStateOperationService {

    /**
     * @return A list only containing the color state service type.
     */
    @Override
    default List<ServiceType> getRepresentingOperationServiceTypes() {
        return Collections.singletonList(ServiceType.COLOR_STATE_SERVICE);
    }

    static Color detectNeutralWhiteColor(final UnitConfig config, final Logger logger) throws InterruptedException {
        try {
            final MetaConfigPool configPool = new MetaConfigPool();
            configPool.register(new MetaConfigVariableProvider("UnitConfig", config.getMetaConfig()));

            // add meta config of service config with type ColorStateService
            ServiceConfig colorStateServiceConfig = null;
            for (ServiceConfig serviceConfig : config.getServiceConfigList()) {
                if (serviceConfig.getServiceDescription().getServiceType() == COLOR_STATE_SERVICE) {
                    colorStateServiceConfig = serviceConfig;
                }
            }
            if (colorStateServiceConfig != null) {
                configPool.register(new MetaConfigVariableProvider("ServiceConfig", colorStateServiceConfig.getMetaConfig()));
            }

            try {
                // add meta config of device
                UnitConfig deviceUnitConfig = Registries.getUnitRegistry(true).getUnitConfigById(config.getUnitHostId());
                configPool.register(new MetaConfigVariableProvider("DeviceUnitConfig", deviceUnitConfig.getMetaConfig()));

                try {
                    // add meta config of device class
                    Registries.waitForData();
                    DeviceClassType.DeviceClass deviceClass = Registries.getClassRegistry().getDeviceClassById(deviceUnitConfig.getDeviceConfig().getDeviceClassId());
                    configPool.register(new MetaConfigVariableProvider("DeviceClass", deviceClass.getMetaConfig()));


                    // add meta config of service template config in unit template of deviceClass
                    ServiceTemplateConfigType.ServiceTemplateConfig colorStateServiceTemplateConfig = null;
                    for (UnitTemplateConfigType.UnitTemplateConfig unitTemplateConfig : deviceClass.getUnitTemplateConfigList()) {
                        if (unitTemplateConfig.getId().equals(config.getUnitTemplateConfigId())) {
                            for (ServiceTemplateConfigType.ServiceTemplateConfig serviceTempalteConfig : unitTemplateConfig.getServiceTemplateConfigList()) {
                                if (serviceTempalteConfig.getServiceType() == ServiceTemplateType.ServiceTemplate.ServiceType.COLOR_STATE_SERVICE) {
                                    colorStateServiceTemplateConfig = serviceTempalteConfig;
                                }
                            }
                            break;
                        }
                    }
                    if (colorStateServiceTemplateConfig != null) {
                        configPool.register(new MetaConfigVariableProvider("ServiceTemplateConfig", colorStateServiceTemplateConfig.getMetaConfig()));
                    }
                } catch (NotAvailableException ex) {
                    logger.warn("Could not check deviceClass of device[" + ScopeProcessor.generateStringRep(deviceUnitConfig.getScope()) + "] for neutral white because its not available");
                }
            } catch (NotAvailableException ex) {
                logger.warn("Could not check host of colorableLight[" + ScopeProcessor.generateStringRep(config.getScope()) + "] for neutral white because its not available");
            }

            try {
                String neutralWhiteString = configPool.getValue(NEUTRAL_WHITE_KEY);
                try {
                    String[] split = neutralWhiteString.replace(" ", "").split(",");
                    if (split.length != 3) {
                        throw new CouldNotPerformException("NeutralWhite for [" + ScopeProcessor.generateStringRep(config.getScope()) + "] has the wrong number of parameters!");
                    }
                    double hue = Double.parseDouble(split[0]);
                    double saturation = Double.parseDouble(split[1]);
                    double brightness = Double.parseDouble(split[2]);
                    HSBColor hsbColor = HSBColor.newBuilder().setHue(hue).setSaturation(saturation).setBrightness(brightness).build();
                    return Color.newBuilder().setType(Color.Type.HSB).setHsbColor(hsbColor).build();
                } catch (CouldNotPerformException ex) {
                    throw new NotAvailableException("Color", "NeutralWhite", new CouldNotPerformException("Could not parse [" + neutralWhiteString + "] as neutral white! Please define as <h, s, b>"));
                } catch (NumberFormatException ex) {
                    throw new NotAvailableException("Color", "NeutralWhite", new CouldNotPerformException("Could not parse [" + neutralWhiteString + "] as doubles and thus as NeutralWhite!"));
                }
            } catch (NotAvailableException ex) {
                // just use the default value...
            }
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not find NeutralWhite!", ex), logger);
        }
        return ColorableLight.DEFAULT_NEUTRAL_WHITE_COLOR;
    }
}
