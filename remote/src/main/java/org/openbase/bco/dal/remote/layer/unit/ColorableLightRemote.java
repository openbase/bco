package org.openbase.bco.dal.remote.layer.unit;

/*
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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

import org.openbase.bco.dal.lib.layer.unit.ColorableLight;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.extension.type.processing.MetaConfigPool;
import org.openbase.jul.extension.type.processing.MetaConfigVariableProvider;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.service.ServiceConfigType;
import org.openbase.type.domotic.service.ServiceTemplateConfigType;
import org.openbase.type.domotic.service.ServiceTemplateType;
import org.openbase.type.domotic.state.BrightnessStateType.BrightnessState;
import org.openbase.type.domotic.state.ColorStateType;
import org.openbase.type.domotic.state.PowerStateType;
import org.openbase.type.domotic.unit.UnitConfigType;
import org.openbase.type.domotic.unit.UnitTemplateConfigType;
import org.openbase.type.domotic.unit.dal.ColorableLightDataType;
import org.openbase.type.domotic.unit.dal.ColorableLightDataType.ColorableLightData;
import org.openbase.type.domotic.unit.device.DeviceClassType;
import org.openbase.type.vision.ColorType;
import org.openbase.type.vision.HSBColorType.HSBColor;
import org.openbase.type.vision.RGBColorType.RGBColor;

import java.util.concurrent.Future;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class ColorableLightRemote extends AbstractUnitRemote<ColorableLightData> implements ColorableLight {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ColorableLightDataType.ColorableLightData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ColorStateType.ColorState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerStateType.PowerState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ColorType.Color.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(HSBColor.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(RGBColor.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(BrightnessState.getDefaultInstance()));
    }

    private ColorType.Color neutralWhite = DEFAULT_NEUTRAL_WHITE_COLOR;

    public ColorableLightRemote() {
        super(ColorableLightData.class);
    }

    @Override
    public UnitConfigType.UnitConfig applyConfigUpdate(final UnitConfigType.UnitConfig unitConfig) throws CouldNotPerformException, InterruptedException {
        updateNeutralWhiteValue(unitConfig);
        return super.applyConfigUpdate(unitConfig);
    }

    public void updateNeutralWhiteValue(final UnitConfigType.UnitConfig config) throws InterruptedException {
        try {
            final MetaConfigPool configPool = new MetaConfigPool();
            configPool.register(new MetaConfigVariableProvider("UnitConfig", config.getMetaConfig()));

            // add meta config of service config with type ColorStateService
            ServiceConfigType.ServiceConfig colorStateServiceConfig = null;
            for (ServiceConfigType.ServiceConfig serviceConfig : config.getServiceConfigList()) {
                if (serviceConfig.getServiceDescription().getServiceType() == ServiceTemplateType.ServiceTemplate.ServiceType.COLOR_STATE_SERVICE) {
                    colorStateServiceConfig = serviceConfig;
                }
            }
            if (colorStateServiceConfig != null) {
                configPool.register(new MetaConfigVariableProvider("ServiceConfig", colorStateServiceConfig.getMetaConfig()));
            }

            try {
                // add meta config of device
                UnitConfigType.UnitConfig deviceUnitConfig = Registries.getUnitRegistry(true).getUnitConfigById(config.getUnitHostId());
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
                    logger.warn("Could not check deviceClass of device[" + ScopeGenerator.generateStringRep(deviceUnitConfig.getScope()) + "] for neutral white because its not available");
                }
            } catch (NotAvailableException ex) {
                logger.warn("Could not check host of colorableLight[" + ScopeGenerator.generateStringRep(config.getScope()) + "] for neutral white because its not available");
            }

            try {
                String neutralWhiteString = configPool.getValue(NEUTRAL_WHITE_KEY);
                try {
                    String[] split = neutralWhiteString.replace(" ", "").split(",");
                    if (split.length != 3) {
                        throw new CouldNotPerformException("NeutralWhite for [" + ScopeGenerator.generateStringRep(config.getScope()) + "] has the wrong number of parameters!");
                    }
                    double hue = Double.parseDouble(split[0]);
                    double saturation = Double.parseDouble(split[1]);
                    double brightness = Double.parseDouble(split[2]);
                    HSBColor hsbColor = HSBColor.newBuilder().setHue(hue).setSaturation(saturation).setBrightness(brightness).build();
                    neutralWhite = ColorType.Color.newBuilder().setType(ColorType.Color.Type.HSB).setHsbColor(hsbColor).build();
                } catch (CouldNotPerformException ex) {
                    throw new NotAvailableException("Color", "NeutralWhite", new CouldNotPerformException("Could not parse [" + neutralWhiteString + "] as neutral white! Please define as <h, s, b>"));
                } catch (NumberFormatException ex) {
                    throw new NotAvailableException("Color", "NeutralWhite", new CouldNotPerformException("Could not parse [" + neutralWhiteString + "] as doubles and thus as NeutralWhite!"));
                }
            } catch (NotAvailableException ex) {
                neutralWhite = DEFAULT_NEUTRAL_WHITE_COLOR;
            }
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not find NeutralWhite!", ex), logger);
        }
    }

    @Override
    public Future<ActionDescription> setNeutralWhite() throws CouldNotPerformException {
        return setColor(neutralWhite);
    }
}
