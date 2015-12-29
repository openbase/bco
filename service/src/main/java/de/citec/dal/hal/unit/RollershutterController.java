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
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.protobuf.ClosableDataBuilder;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.ShutterStateType.ShutterState;
import rst.homeautomation.unit.RollershutterType.Rollershutter;
import rst.homeautomation.unit.UnitConfigType;

/**
 *
 * @author thuxohl
 */
public class RollershutterController extends AbstractUnitController<Rollershutter, Rollershutter.Builder> implements RollershutterInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(Rollershutter.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ShutterState.getDefaultInstance()));
    }

    private final ShutterService shutterService;
    private final OpeningRatioService openingRatioService;

    public RollershutterController(final UnitConfigType.UnitConfig config, Device device, Rollershutter.Builder builder) throws InstantiationException, CouldNotPerformException {
        this(config, device, builder, device.getServiceFactory());
    }

    public RollershutterController(final UnitConfigType.UnitConfig config, Device device, Rollershutter.Builder builder, final ServiceFactory serviceFactory) throws InstantiationException, CouldNotPerformException {
        super(config, RollershutterController.class, device, builder);
        this.shutterService = serviceFactory.newShutterService(device, this);
        this.openingRatioService = serviceFactory.newOpeningRatioService(device, this);
    }

    public void updateShutter(final ShutterState.State value) throws CouldNotPerformException {
        logger.debug("Apply shutter Update[" + value + "] for " + this + ".");

        try (ClosableDataBuilder<Rollershutter.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().getShutterStateBuilder().setValue(value);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply shutter Update[" + value + "] for " + this + "!", ex);
        }
    }

    @Override
    public void setShutter(final ShutterState.State state) throws CouldNotPerformException {
        logger.debug("Setting [" + getLabel() + "] to ShutterState [" + state.name() + "]");
        this.shutterService.setShutter(state);
    }

    @Override
    public ShutterState getShutter() throws NotAvailableException {
        try {
            return getData().getShutterState();
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
