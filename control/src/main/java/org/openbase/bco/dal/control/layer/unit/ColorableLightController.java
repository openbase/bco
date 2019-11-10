package org.openbase.bco.dal.control.layer.unit;

/*
 * #%L
 * BCO DAL Control
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
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

import org.openbase.bco.dal.lib.layer.unit.ColorableLight;
import org.openbase.bco.dal.lib.layer.unit.HostUnitController;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.MultiException.ExceptionStack;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.StackTracePrinter;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.type.processing.ScopeProcessor;
import org.openbase.jul.extension.type.processing.MetaConfigPool;
import org.openbase.jul.extension.type.processing.MetaConfigVariableProvider;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.service.ServiceConfigType.ServiceConfig;
import org.openbase.type.domotic.service.ServiceTemplateConfigType.ServiceTemplateConfig;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.BrightnessStateType.BrightnessState;
import org.openbase.type.domotic.state.ColorStateType.ColorState;
import org.openbase.type.domotic.state.PowerStateType.PowerState;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateConfigType.UnitTemplateConfig;
import org.openbase.type.domotic.unit.dal.ColorableLightDataType.ColorableLightData;
import org.openbase.type.domotic.unit.device.DeviceClassType.DeviceClass;
import org.openbase.type.vision.ColorType.Color;
import org.openbase.type.vision.HSBColorType.HSBColor;
import org.openbase.type.vision.RGBColorType.RGBColor;

import java.util.concurrent.Future;

import static org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.*;

/**
 * @author Tamino Huxohl
 * @author Marian Pohling
 */
public class ColorableLightController extends AbstractDALUnitController<ColorableLightData, ColorableLightData.Builder> implements ColorableLight {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ColorableLightData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(HSBColor.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(RGBColor.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(Color.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ColorState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(BrightnessState.getDefaultInstance()));
    }

    private Color neutralWhite;

    public ColorableLightController(final HostUnitController hostUnitController, final ColorableLightData.Builder builder) throws InstantiationException {
        super(hostUnitController, builder);
        this.neutralWhite = ColorableLight.DEFAULT_NEUTRAL_WHITE_COLOR;
    }

    @Override
    public UnitConfig applyConfigUpdate(final UnitConfig config) throws CouldNotPerformException, InterruptedException {
        updateNeutralWhiteValue(config);
        return super.applyConfigUpdate(config);
    }

    public void updateNeutralWhiteValue(final UnitConfig config) throws InterruptedException {
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

            // add meta config of device
            UnitConfig deviceUnitConfig = Registries.getUnitRegistry(true).getUnitConfigById(config.getUnitHostId());
            configPool.register(new MetaConfigVariableProvider("DeviceUnitConfig", deviceUnitConfig.getMetaConfig()));

            // add meta config of device class
            Registries.waitForData();
            DeviceClass deviceClass = Registries.getClassRegistry().getDeviceClassById(deviceUnitConfig.getDeviceConfig().getDeviceClassId());
            configPool.register(new MetaConfigVariableProvider("DeviceClass", deviceClass.getMetaConfig()));

            // add meta config of service template config in unit template of deviceClass
            ServiceTemplateConfig colorStateServiceTemplateConfig = null;
            for (UnitTemplateConfig unitTemplateConfig : deviceClass.getUnitTemplateConfigList()) {
                if (unitTemplateConfig.getId().equals(config.getUnitTemplateConfigId())) {
                    for (ServiceTemplateConfig serviceTempalteConfig : unitTemplateConfig.getServiceTemplateConfigList()) {
                        if (serviceTempalteConfig.getServiceType() == COLOR_STATE_SERVICE) {
                            colorStateServiceTemplateConfig = serviceTempalteConfig;
                        }
                    }
                    break;
                }
            }
            if (colorStateServiceTemplateConfig != null) {
                configPool.register(new MetaConfigVariableProvider("ServiceTemplateConfig", colorStateServiceTemplateConfig.getMetaConfig()));
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
                    neutralWhite = Color.newBuilder().setType(Color.Type.HSB).setHsbColor(hsbColor).build();
                } catch (CouldNotPerformException ex) {
                    throw new NotAvailableException("Color", "NeutralWhite", new CouldNotPerformException("Could not parse [" + neutralWhiteString + "] as neutral white! Please define as <h, s, b>"));
                } catch (NumberFormatException ex) {
                    throw new NotAvailableException("Color", "NeutralWhite", new CouldNotPerformException("Could not parse [" + neutralWhiteString + "] as doubles and thus as NeutralWhite!"));
                }
            } catch (NotAvailableException ex) {
                neutralWhite = ColorableLight.DEFAULT_NEUTRAL_WHITE_COLOR;
            }
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not find NeutralWhite!", ex), logger);
        }
    }

    @Override
    public Future<ActionDescription> setNeutralWhite() {
        return setColor(neutralWhite);
    }

    @Override
    protected void applyCustomDataUpdate(ColorableLightData.Builder internalBuilder, ServiceType serviceType) {
        switch (serviceType) {
            case COLOR_STATE_SERVICE:

                updateLastWithCurrentState(BRIGHTNESS_STATE_SERVICE, internalBuilder);
                updateLastWithCurrentState(POWER_STATE_SERVICE, internalBuilder);

                final HSBColor hsbColor = internalBuilder.getColorState().getColor().getHsbColor();
                internalBuilder.getBrightnessStateBuilder().setBrightness(hsbColor.getBrightness());

                if (hsbColor.getBrightness() == 0d) {
                    internalBuilder.getPowerStateBuilder().setValue(PowerState.State.OFF);
                } else {
                    internalBuilder.getPowerStateBuilder().setValue(PowerState.State.ON);
                }

                copyResponsibleAction(COLOR_STATE_SERVICE, BRIGHTNESS_STATE_SERVICE, internalBuilder);
                copyResponsibleAction(COLOR_STATE_SERVICE, POWER_STATE_SERVICE, internalBuilder);

                break;
            case BRIGHTNESS_STATE_SERVICE:

                updateLastWithCurrentState(COLOR_STATE_SERVICE, internalBuilder);
                updateLastWithCurrentState(POWER_STATE_SERVICE, internalBuilder);

                // sync color brightness state.
                internalBuilder.getColorStateBuilder().getColorBuilder().getHsbColorBuilder().setBrightness(internalBuilder.getBrightnessState().getBrightness());

                // sync power state
                if (internalBuilder.getBrightnessState().getBrightness() == 0d) {
                    internalBuilder.getPowerStateBuilder().setValue(PowerState.State.OFF);
                } else {
                    internalBuilder.getPowerStateBuilder().setValue(PowerState.State.ON);
                }

                copyResponsibleAction(BRIGHTNESS_STATE_SERVICE, COLOR_STATE_SERVICE, internalBuilder);
                copyResponsibleAction(BRIGHTNESS_STATE_SERVICE, POWER_STATE_SERVICE, internalBuilder);

                break;
            case POWER_STATE_SERVICE:

                updateLastWithCurrentState(COLOR_STATE_SERVICE, internalBuilder);
                updateLastWithCurrentState(BRIGHTNESS_STATE_SERVICE, internalBuilder);

                // sync brightness and color state.
                switch (internalBuilder.getPowerState().getValue()) {
                    case ON:
                        if (internalBuilder.getBrightnessStateBuilder().getBrightness() == 0d) {
                            internalBuilder.getBrightnessStateBuilder().setBrightness(1d);
                        }
                        if (internalBuilder.getColorStateBuilder().getColorBuilder().getHsbColorBuilder().getBrightness() == 0d) {
                            internalBuilder.getColorStateBuilder().getColorBuilder().getHsbColorBuilder().setBrightness(1d);
                        }
                        break;
                    case OFF:
                        internalBuilder.getBrightnessStateBuilder().setBrightness(0d);
                        internalBuilder.getColorStateBuilder().getColorBuilder().getHsbColorBuilder().setBrightness(0d);
                        break;
                    default:
                        break;
                }

                copyResponsibleAction(POWER_STATE_SERVICE, COLOR_STATE_SERVICE, internalBuilder);
                copyResponsibleAction(POWER_STATE_SERVICE, BRIGHTNESS_STATE_SERVICE, internalBuilder);

                break;
        }
    }
}
