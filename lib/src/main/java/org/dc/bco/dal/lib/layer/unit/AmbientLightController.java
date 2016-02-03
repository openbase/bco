/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.lib.layer.unit;

import org.dc.bco.dal.lib.layer.service.BrightnessService;
import org.dc.bco.dal.lib.layer.service.ColorService;
import org.dc.bco.dal.lib.layer.service.PowerService;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.protobuf.ClosableDataBuilder;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.PowerStateType.PowerState;
import rst.homeautomation.unit.AmbientLightType.AmbientLight;
import rst.vision.HSVColorType.HSVColor;

/**
 *
 * @author Tamino Huxohl
 * @author Marian Pohling
 */
public class AmbientLightController extends AbstractUnitController<AmbientLight, AmbientLight.Builder> implements AmbientLightInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AmbientLight.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(HSVColor.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerState.getDefaultInstance()));
    }

    private final ColorService colorService;
    private final BrightnessService brightnessService;
    private final PowerService powerService;

    public AmbientLightController(final UnitHost unitHost, final AmbientLight.Builder builder) throws InstantiationException, CouldNotPerformException {
        super(AmbientLightController.class, unitHost, builder);
        this.powerService = getServiceFactory().newPowerService(this);
        this.colorService = getServiceFactory().newColorService(this);
        this.brightnessService = getServiceFactory().newBrightnessService(this);
    }

    public void updatePower(final PowerState.State value) throws CouldNotPerformException {
        logger.debug("Apply power Update[" + value + "] for " + this + ".");

        try (ClosableDataBuilder<AmbientLight.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().getPowerStateBuilder().setValue(value);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply power Update[" + value + "] for " + this + "!", ex);
        }
    }

    @Override
    public void setPower(final PowerState.State state) throws CouldNotPerformException {
        logger.debug("Set " + getType().name() + "[" + getLabel() + "] to PowerState [" + state.name() + "]");
        powerService.setPower(state);
    }

    @Override
    public PowerState getPower() throws NotAvailableException {
        try {
            return getData().getPowerState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("power", ex);
        }
    }

    public void updateColor(final HSVColor value) throws CouldNotPerformException {
        logger.debug("Apply color Update[" + value + "] for " + this + ".");

        try (ClosableDataBuilder<AmbientLight.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setColor(value);
            dataBuilder.getInternalBuilder().getPowerStateBuilder().setValue(PowerState.State.ON);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply color Update[" + value + "] for " + this + "!", ex);
        }
    }

    @Override
    public void setColor(final HSVColor color) throws CouldNotPerformException {
        logger.debug("Set " + getType().name() + "[" + getLabel() + "] to HSVColor[" + color.getHue() + "|" + color.getSaturation() + "|" + color.getValue() + "]");
        colorService.setColor(color);
    }

    @Override
    public HSVColor getColor() throws NotAvailableException {
        try {
            logger.info("===================== getcolor request");
            return getData().getColor();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("color", ex);
        }
    }

    public void updateBrightness(Double value) throws CouldNotPerformException {
        logger.info("Apply brightness Update[" + value + "] for " + this + ".");

        try (ClosableDataBuilder<AmbientLight.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setColor(dataBuilder.getInternalBuilder().getColor().toBuilder().setValue(value).build());
            if(value == 0) {
                dataBuilder.getInternalBuilder().getPowerStateBuilder().setValue(PowerState.State.OFF);
            } else {
                dataBuilder.getInternalBuilder().getPowerStateBuilder().setValue(PowerState.State.ON);
            }
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply brightness Update[" + value + "] for " + this + "!", ex);
        }
    }

    @Override
    public void setBrightness(Double brightness) throws CouldNotPerformException {
        logger.debug("Set " + getType().name() + "[" + getLabel() + "] to Brightness[" + brightness + "]");
        brightnessService.setBrightness(brightness);
    }

    @Override
    public Double getBrightness() throws NotAvailableException {
        try {
            return getData().getColor().getValue();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("brightness", ex);
        }
    }
}
