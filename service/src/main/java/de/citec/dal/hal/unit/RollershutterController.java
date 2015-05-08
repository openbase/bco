/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.unit;

import de.citec.dal.hal.device.Device;
import de.citec.dal.hal.service.OpeningRatioService;
import de.citec.dal.hal.service.ServiceFactory;
import de.citec.dal.hal.service.ShutterService;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.extension.protobuf.ClosableDataBuilder;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.ShutterType;
import rst.homeautomation.state.ShutterType.Shutter;
import rst.homeautomation.unit.RollershutterType;
import rst.homeautomation.unit.RollershutterType.Rollershutter;
import rst.homeautomation.unit.UnitConfigType;

/**
 *
 * @author thuxohl
 */
public class RollershutterController extends AbstractUnitController<Rollershutter, Rollershutter.Builder> implements RollershutterInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(RollershutterType.Rollershutter.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ShutterType.Shutter.getDefaultInstance()));
    }

    private final ShutterService shutterService;
    private final OpeningRatioService openingRatioService;

    public RollershutterController(final UnitConfigType.UnitConfig config, Device device, RollershutterType.Rollershutter.Builder builder) throws InstantiationException, CouldNotPerformException {
        this(config, device, builder, device.getDefaultServiceFactory());
    }

    public RollershutterController(final UnitConfigType.UnitConfig config, Device device, RollershutterType.Rollershutter.Builder builder, final ServiceFactory serviceFactory) throws InstantiationException, CouldNotPerformException {
        super(config, RollershutterController.class, device, builder);
        this.shutterService = serviceFactory.newShutterService(device, this);
        this.openingRatioService = serviceFactory.newOpeningRatioService(device, this);
    }

    public void updateShutter(final ShutterType.Shutter.ShutterState value) throws CouldNotPerformException {
        logger.debug("Apply shutter Update[" + value + "] for " + this + ".");

        try (ClosableDataBuilder<Rollershutter.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().getShutterStateBuilder().setState(value);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply shutter Update[" + value + "] for " + this + "!", ex);
        }
    }

    @Override
    public void setShutter(final ShutterType.Shutter.ShutterState state) throws CouldNotPerformException {
        logger.debug("Setting [" + getLabel() + "] to ShutterState [" + state.name() + "]");
        this.shutterService.setShutter(state);
    }

    @Override
    public Shutter.ShutterState getShutter() throws NotAvailableException {
        try {
            return getData().getShutterState().getState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("shutter", ex);
        }
    }

    public void updateOpeningRatio(final Double value) throws CouldNotPerformException {
        logger.debug("Apply opening ratio Update[" + value + "] for " + this + ".");

        try (ClosableDataBuilder<Rollershutter.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setOpeningRatio(value);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply opening ratio Update[" + value + "] for " + this + "!", ex);
        }
    }

    @Override
    public void setOpeningRatio(Double openingRatio) throws CouldNotPerformException {
        logger.debug("Setting [" + getLabel() + "] to OpeningRatio [" + openingRatio + "]");
        this.openingRatioService.setOpeningRatio(openingRatio);
    }

    @Override
    public Double getOpeningRatio() throws NotAvailableException {
        try {
            return getData().getOpeningRatio();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("opening ratio", ex);
        }
    }
}
