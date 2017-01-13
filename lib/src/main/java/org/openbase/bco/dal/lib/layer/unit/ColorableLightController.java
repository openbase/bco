package org.openbase.bco.dal.lib.layer.unit;

/*
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import org.openbase.bco.dal.lib.layer.service.operation.BrightnessStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.ColorStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.PowerStateOperationService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.state.BrightnessStateType.BrightnessState;
import rst.domotic.state.ColorStateType.ColorState;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.unit.dal.ColorableLightDataType.ColorableLightData;
import rst.domotic.unit.UnitConfigType;
import rst.vision.ColorType.Color;
import rst.vision.HSBColorType.HSBColor;
import rst.vision.RGBColorType.RGBColor;

/**
 *
 * * @author Tamino Huxohl
 * * @author Marian Pohling
 */
public class ColorableLightController extends AbstractUnitController<ColorableLightData, ColorableLightData.Builder> implements ColorableLight {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ColorableLightData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(HSBColor.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(RGBColor.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(Color.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ColorState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerState.getDefaultInstance()));
    }

    private ColorStateOperationService colorService;
    private BrightnessStateOperationService brightnessService;
    private PowerStateOperationService powerService;

    public ColorableLightController(final UnitHost unitHost, final ColorableLightData.Builder builder) throws InstantiationException, CouldNotPerformException {
        super(ColorableLightController.class, unitHost, builder);
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

    public void updatePowerStateProvider(final PowerState value) throws CouldNotPerformException {
        logger.debug("Apply powerState Update[" + value + "] for " + this + ".");

        try (ClosableDataBuilder<ColorableLightData.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setPowerState(value);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply powerState Update[" + value + "] for " + this + "!", ex);
        }
    }

    @Override
    public Future<Void> setPowerState(final PowerState state) throws CouldNotPerformException {
        logger.debug("Set " + getType().name() + "[" + getLabel() + "] to PowerState [" + state + "]");
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

    public void updateColorStateProvider(final ColorState colorState) throws CouldNotPerformException {
        logger.debug("Apply colorState Update[" + colorState + "] for " + this + ".");

        try (ClosableDataBuilder<ColorableLightData.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setColorState(colorState);
            dataBuilder.getInternalBuilder().getPowerStateBuilder().setValue(PowerState.State.ON);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply colorState Update[" + colorState + "] for " + this + "!", ex);
        }
    }

    @Override
    public Future<Void> setColorState(final ColorState colorState) throws CouldNotPerformException {
        return colorService.setColorState(colorState);
    }

    @Override
    public ColorState getColorState() throws NotAvailableException {
        try {
            return getData().getColorState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("colorState", ex);
        }
    }

    public void updateBrightnessStateProvider(BrightnessState brightnessState) throws CouldNotPerformException {
        logger.debug("Apply brightnessState Update[" + brightnessState + "] for " + this + ".");

        try (ClosableDataBuilder<ColorableLightData.Builder> dataBuilder = getDataBuilder(this)) {
            HSBColor hsb = dataBuilder.getInternalBuilder().getColorState().getColor().getHsbColor().toBuilder().setBrightness(brightnessState.getBrightness()).build();
            Color color = Color.newBuilder().setType(Color.Type.HSB).setHsbColor(hsb).build();
            dataBuilder.getInternalBuilder().setColorState(dataBuilder.getInternalBuilder().getColorState().toBuilder().setColor(color).build());
            if (brightnessState.getBrightness() == 0) {
                dataBuilder.getInternalBuilder().getPowerStateBuilder().setValue(PowerState.State.OFF);
            } else {
                dataBuilder.getInternalBuilder().getPowerStateBuilder().setValue(PowerState.State.ON);
            }
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply brightnessState Update[" + brightnessState + "] for " + this + "!", ex);
        }
    }

    @Override
    public Future<Void> setBrightnessState(BrightnessState brightnessState) throws CouldNotPerformException {
        logger.debug("Set " + getType().name() + "[" + getLabel() + "] to BrightnessState[" + brightnessState + "]");
        return brightnessService.setBrightnessState(brightnessState);
    }

    @Override
    public BrightnessState getBrightnessState() throws NotAvailableException {
        try {
            return BrightnessState.newBuilder().setBrightness(getData().getColorState().getColor().getHsbColor().getBrightness()).build();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("brightnessState", ex);
        }
    }
}
