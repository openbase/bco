package org.openbase.bco.dal.lib.layer.unit;

/*
 * #%L
 * BCO DAL Library
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
import java.util.concurrent.Future;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.service.operation.BrightnessStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.PowerStateOperationService;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.extension.rst.processing.MetaConfigPool;
import org.openbase.jul.extension.rst.processing.MetaConfigVariableProvider;
import org.openbase.jul.schedule.FutureProcessor;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.service.ServiceTemplateConfigType.ServiceTemplateConfig;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.BrightnessStateType.BrightnessState;
import rst.domotic.state.ColorStateType.ColorState;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.unit.UnitConfigType;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateConfigType.UnitTemplateConfig;
import rst.domotic.unit.dal.ColorableLightDataType.ColorableLightData;
import rst.domotic.unit.device.DeviceClassType.DeviceClass;
import rst.vision.ColorType.Color;
import rst.vision.HSBColorType.HSBColor;
import rst.vision.RGBColorType.RGBColor;
import org.openbase.bco.dal.lib.layer.service.operation.ColorStateOperationService;
import org.openbase.jul.extension.rst.transform.HSBColorToRGBColorTransformer;

/**
 *
 * * @author Tamino Huxohl
 * * @author Marian Pohling
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

    private ColorStateOperationService colorService;
    private BrightnessStateOperationService brightnessService;
    private PowerStateOperationService powerService;
    private Color neutralWhite;

    public ColorableLightController(final UnitHost unitHost, final ColorableLightData.Builder builder) throws InstantiationException, CouldNotPerformException {
        super(ColorableLightController.class, unitHost, builder);
        this.neutralWhite = ColorableLight.DEFAULT_NEUTRAL_WHITE_COLOR;
    }

    @Override
    public void init(UnitConfigType.UnitConfig config) throws InitializationException, InterruptedException {
        super.init(config);
        try {
            this.powerService = getServiceFactory().newPowerService(this);
            this.colorService = getServiceFactory().newColorService(this);
            this.brightnessService = getServiceFactory().newBrightnessService(this);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
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
                if (serviceConfig.getServiceDescription().getType() == ServiceType.COLOR_STATE_SERVICE) {
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
            Registries.getDeviceRegistry().waitForData();
            DeviceClass deviceClass = Registries.getDeviceRegistry().getDeviceClassById(deviceUnitConfig.getDeviceConfig().getDeviceClassId());
            configPool.register(new MetaConfigVariableProvider("DeviceClass", deviceClass.getMetaConfig()));

            // add meta config of service template config in unit template of deviceClass
            ServiceTemplateConfig colorStateServiceTemplateConfig = null;
            for (UnitTemplateConfig unitTemplateConfig : deviceClass.getUnitTemplateConfigList()) {
                if (unitTemplateConfig.getId().equals(config.getUnitTemplateConfigId())) {
                    for (ServiceTemplateConfig serviceTempalteConfig : unitTemplateConfig.getServiceTemplateConfigList()) {
                        if (serviceTempalteConfig.getServiceType() == ServiceType.COLOR_STATE_SERVICE) {
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
                        throw new CouldNotPerformException("NeutralWhite for [" + ScopeGenerator.generateStringRep(config.getScope()) + "] has the wrong number of parameters!");
                    }
                    double hue = Double.parseDouble(split[0]);
                    double saturation = Double.parseDouble(split[1]);
                    double brightness = Double.parseDouble(split[2]);
                    HSBColor hsbColor = HSBColor.newBuilder().setHue(hue).setSaturation(saturation).setBrightness(brightness).build();
                    neutralWhite = Color.newBuilder().setType(Color.Type.HSB).setHsbColor(hsbColor).build();
                } catch (CouldNotPerformException ex) {
                    logger.warn("Could not parse [" + neutralWhiteString + "] as neutral white! Please define as <h, s, b>", ex);
                    throw new NotAvailableException("NeutralWhite");
                } catch (NumberFormatException ex) {
                    logger.warn("Could not parse [" + neutralWhiteString + "] as doubles and thus as NeutralWhite!", ex);
                    throw new NotAvailableException("NeutralWhite");
                }
            } catch (NotAvailableException ex) {
                neutralWhite = ColorableLight.DEFAULT_NEUTRAL_WHITE_COLOR;
            }
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not find NeutralWhite!", ex), logger);
        }
    }

    @Override
    public Future<ActionFuture> setNeutralWhite() throws CouldNotPerformException {
        return setColor(neutralWhite);
    }

//    public void updatePowerStateProvider(final PowerState value) throws CouldNotPerformException {
//        logger.debug("Apply powerState Update[" + value + "] for " + this + ".");
//
//        try (ClosableDataBuilder<ColorableLightData.Builder> dataBuilder = getDataBuilder(this)) {
//            // move current state to last state
//            dataBuilder.getInternalBuilder().setPowerStateLast(dataBuilder.getInternalBuilder().getPowerState());
//            
//            PowerState newState;
//            if (value.getValue() ==  dataBuilder.getInternalBuilder().getPowerStateRequested().getValue()) {
//                newState = dataBuilder.getInternalBuilder().getPowerStateRequested();
//            } else {
//                newState = value;
//            }
//            ActionDescription updatedResponsibleAction = newState.getResponsibleAction().toBuilder().setTransactionId(generateTransactionId()).build();
//            dataBuilder.getInternalBuilder().setPowerState(newState.toBuilder().setResponsibleAction(updatedResponsibleAction));
//        } catch (Exception ex) {
//            throw new CouldNotPerformException("Could not apply powerState Update[" + value + "] for " + this + "!", ex);
//        }
//    }
    @Override
    public Future<ActionFuture> setPowerState(final PowerState state) throws CouldNotPerformException {
        logger.debug("Set " + getUnitType().name() + "[" + getLabel() + "] to PowerState [" + state + "]");
        try {
            Services.verifyOperationServiceState(state);
        } catch (VerificationFailedException ex) {
            return FutureProcessor.canceledFuture(ActionFuture.class, ex);
        }
        return powerService.setPowerState(state);
    }

    @Override
    public PowerState getPowerState() throws NotAvailableException {
        try {
            return getData().getPowerState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("powerState", ex);
        }
    }

//    public void updateColorStateProvider(final ColorState colorState) throws CouldNotPerformException {
//        logger.debug("Apply colorState Update[" + colorState + "] for " + this + ".");
//
//        try (ClosableDataBuilder<ColorableLightData.Builder> dataBuilder = getDataBuilder(this)) {
//            long transactionId = dataBuilder.getInternalBuilder().getColorState().getTransactionId() + 1;
//            dataBuilder.getInternalBuilder().setColorState(colorState.toBuilder().setTransactionId(transactionId));
//
//            BrightnessState brightnessState = BrightnessState.newBuilder().setBrightness(colorState.getColor().getHsbColor().getBrightness()).build();
//            dataBuilder.getInternalBuilder().setBrightnessState(brightnessState);
//            dataBuilder.getInternalBuilder().getPowerStateBuilder().setValue(PowerState.State.ON);
//        } catch (Exception ex) {
//            throw new CouldNotPerformException("Could not apply colorState Update[" + colorState + "] for " + this + "!", ex);
//        }
//    }
    @Override
    public Future<ActionFuture> setColorState(final ColorState state) throws CouldNotPerformException {
        try {
            Services.verifyOperationServiceState(state);
        } catch (VerificationFailedException ex) {
            return FutureProcessor.canceledFuture(ActionFuture.class, ex);
        }
        if (state.getColor().getType() == Color.Type.RGB) {
            return colorService.setColor(HSBColorToRGBColorTransformer.transform(state.getColor().getRgbColor()));
        } else {
            return colorService.setColorState(state);
        }
    }

    @Override
    public ColorState getColorState() throws NotAvailableException {
        try {
            return getData().getColorState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("colorState", ex);
        }
    }

//    public void updateBrightnessStateProvider(BrightnessState brightnessState) throws CouldNotPerformException {
//        logger.debug("Apply brightnessState Update[" + brightnessState + "] for " + this + ".");
//
//        try (ClosableDataBuilder<ColorableLightData.Builder> dataBuilder = getDataBuilder(this)) {
//            long transactionId = dataBuilder.getInternalBuilder().getBrightnessState().getTransactionId() + 1;
//            dataBuilder.getInternalBuilder().setBrightnessState(brightnessState.toBuilder().setTransactionId(transactionId));
//
//            HSBColor hsb = dataBuilder.getInternalBuilder().getColorState().getColor().getHsbColor().toBuilder().setBrightness(brightnessState.getBrightness()).build();
//            Color color = Color.newBuilder().setType(Color.Type.HSB).setHsbColor(hsb).build();
//            dataBuilder.getInternalBuilder().setColorState(dataBuilder.getInternalBuilder().getColorState().toBuilder().setColor(color).build());
//
//            if (brightnessState.getBrightness() == 0) {
//                dataBuilder.getInternalBuilder().getPowerStateBuilder().setValue(PowerState.State.OFF);
//            } else {
//                dataBuilder.getInternalBuilder().getPowerStateBuilder().setValue(PowerState.State.ON);
//            }
//        } catch (Exception ex) {
//            throw new CouldNotPerformException("Could not apply brightnessState Update[" + brightnessState + "] for " + this + "!", ex);
//        }
//    }
    @Override
    public Future<ActionFuture> setBrightnessState(final BrightnessState state) throws CouldNotPerformException {
        logger.debug("Set " + getUnitType().name() + "[" + getLabel() + "] to BrightnessState[" + state + "]");
        try {
            Services.verifyOperationServiceState(state);
        } catch (VerificationFailedException ex) {
            return FutureProcessor.canceledFuture(ActionFuture.class, ex);
        }
        return brightnessService.setBrightnessState(state);
    }

    @Override
    public BrightnessState getBrightnessState() throws NotAvailableException {
        try {
            return getData().getBrightnessState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("brightnessState", ex);
        }
    }

    @Override
    protected void applyDataUpdate(ColorableLightData.Builder internalBuilder, ServiceType serviceType) {
        switch (serviceType) {
            case COLOR_STATE_SERVICE:
                BrightnessState brightnessState = BrightnessState.newBuilder().setBrightness(internalBuilder.getColorState().getColor().getHsbColor().getBrightness()).build();
                internalBuilder.setBrightnessState(brightnessState);
                internalBuilder.getPowerStateBuilder().setValue(PowerState.State.ON);
                break;
            case BRIGHTNESS_STATE_SERVICE:
                HSBColor hsb = internalBuilder.getColorState().getColor().getHsbColor().toBuilder().setBrightness(internalBuilder.getBrightnessState().getBrightness()).build();
                Color color = Color.newBuilder().setType(Color.Type.HSB).setHsbColor(hsb).build();
                internalBuilder.setColorState(internalBuilder.getColorState().toBuilder().setColor(color).build());

                if (internalBuilder.getBrightnessState().getBrightness() == 0) {
                    internalBuilder.getPowerStateBuilder().setValue(PowerState.State.OFF);
                } else {
                    internalBuilder.getPowerStateBuilder().setValue(PowerState.State.ON);
                }
                break;
        }
    }
}
