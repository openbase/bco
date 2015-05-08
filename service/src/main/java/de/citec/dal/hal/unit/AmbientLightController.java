/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.unit;

import de.citec.dal.hal.device.Device;
import de.citec.dal.hal.service.BrightnessService;
import de.citec.dal.hal.service.ColorService;
import de.citec.dal.hal.service.PowerService;
import de.citec.dal.hal.service.ServiceFactory;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.extension.protobuf.ClosableDataBuilder;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.PowerType;
import rst.homeautomation.unit.AmbientLightType.AmbientLight;
import rst.homeautomation.unit.UnitConfigType;
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
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerType.Power.getDefaultInstance()));
    }

    private final ColorService colorService;
    private final BrightnessService brightnessService;
    private final PowerService powerService;

    public AmbientLightController(final UnitConfigType.UnitConfig config, final Device device, final AmbientLight.Builder builder) throws InstantiationException, CouldNotPerformException {
        this(config, device, builder, device.getDefaultServiceFactory());
    }

    public AmbientLightController(final UnitConfigType.UnitConfig config, final Device device, final AmbientLight.Builder builder, final ServiceFactory serviceFactory) throws InstantiationException, CouldNotPerformException {
        super(config, AmbientLightController.class, device, builder);
        this.powerService = serviceFactory.newPowerService(device, this);
        this.colorService = serviceFactory.newColorService(device, this);
        this.brightnessService = serviceFactory.newBrightnessService(device, this);
    }

    public void updatePower(final PowerType.Power.PowerState value) throws CouldNotPerformException {
        logger.debug("Apply power Update[" + value + "] for " + this + ".");

        try (ClosableDataBuilder<AmbientLight.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().getPowerStateBuilder().setState(value);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply power Update[" + value + "] for " + this + "!", ex);
        }
    }

    @Override
    public void setPower(final PowerType.Power.PowerState state) throws CouldNotPerformException {
        logger.debug("Set " + getType().name() + "[" + getLabel() + "] to PowerState [" + state.name() + "]");
        powerService.setPower(state);
    }

    @Override
    public PowerType.Power.PowerState getPower() throws NotAvailableException {
        try {
            return getData().getPowerState().getState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("power", ex);
        }
    }

    public void updateColor(final HSVColor value) throws CouldNotPerformException {
        logger.debug("Apply color Update[" + value + "] for " + this + ".");

        try (ClosableDataBuilder<AmbientLight.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setColor(value);
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
            return getData().getColor();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("color", ex);
        }
    }

    public void updateBrightness(Double value) throws CouldNotPerformException {
        logger.debug("Apply brightness Update[" + value + "] for " + this + ".");

        try (ClosableDataBuilder<AmbientLight.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setColor(dataBuilder.getInternalBuilder().getColor().newBuilderForType().setValue(value).build());
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
