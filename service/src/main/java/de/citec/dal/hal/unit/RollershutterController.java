/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.unit;

import de.citec.dal.hal.device.DeviceInterface;
import de.citec.dal.hal.service.OpeningRatioService;
import de.citec.dal.hal.service.ServiceFactory;
import de.citec.dal.hal.service.ShutterService;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.ShutterType;
import rst.homeautomation.unit.RollershutterType;

/**
 *
 * @author thuxohl
 */
public class RollershutterController extends AbstractUnitController<RollershutterType.Rollershutter, RollershutterType.Rollershutter.Builder> implements RollershutterInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(RollershutterType.Rollershutter.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ShutterType.Shutter.getDefaultInstance()));
    }

    private final ShutterService shutterService;
    private final OpeningRatioService openingRatioService;

    public RollershutterController(final String label, DeviceInterface device, RollershutterType.Rollershutter.Builder builder) throws InstantiationException {
        this(label, device, builder, device.getDefaultServiceFactory());
    }

    public RollershutterController(final String label, DeviceInterface device, RollershutterType.Rollershutter.Builder builder, final ServiceFactory serviceFactory) throws InstantiationException {
        super(RollershutterController.class, label, device, builder);
        this.shutterService = serviceFactory.newShutterService(device, this);
        this.openingRatioService = serviceFactory.newOpeningRatioService(device, this);
    }

    public void updateShutter(final ShutterType.Shutter.ShutterState state) {
        data.getShutterStateBuilder().setState(state);
        notifyChange();
    }

    @Override
    public void setShutter(final ShutterType.Shutter.ShutterState state) throws CouldNotPerformException {
        logger.debug("Setting [" + label + "] to ShutterState [" + state.name() + "]");
        this.shutterService.setShutter(state);
    }

    @Override
    public ShutterType.Shutter.ShutterState getShutter() throws CouldNotPerformException {
        return data.getShutterState().getState();
    }

    public void updateOpeningRatio(final Double openingRatio) {
        data.setOpeningRatio(openingRatio);
        notifyChange();
    }

    @Override
    public void setOpeningRatio(Double openingRatio) throws CouldNotPerformException {
        logger.debug("Setting [" + label + "] to OpeningRatio [" + openingRatio + "]");
        this.openingRatioService.setOpeningRatio(openingRatio);
    }

    @Override
    public Double getOpeningRatio() throws CouldNotPerformException {
        return data.getOpeningRatio();
    }
}
