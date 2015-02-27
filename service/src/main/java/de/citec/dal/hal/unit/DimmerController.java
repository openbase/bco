/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.unit;

import de.citec.dal.hal.device.Device;
import de.citec.dal.hal.service.DimmService;
import de.citec.dal.hal.service.PowerService;
import de.citec.dal.hal.service.ServiceFactory;
import de.citec.jul.exception.CouldNotPerformException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.PowerType;
import rst.homeautomation.unit.DimmerType;

/**
 *
 * @author thuxohl
 */
public class DimmerController extends AbstractUnitController<DimmerType.Dimmer, DimmerType.Dimmer.Builder> implements DimmerInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(DimmerType.Dimmer.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerType.Power.getDefaultInstance()));
    }

    private final PowerService powerService;
    private final DimmService dimmService;

    public DimmerController(final String label, Device device, DimmerType.Dimmer.Builder builder) throws de.citec.jul.exception.InstantiationException {
        this(label, device, builder, device.getDefaultServiceFactory());
    }

    public DimmerController(final String label, Device device, DimmerType.Dimmer.Builder builder, final ServiceFactory serviceFactory) throws de.citec.jul.exception.InstantiationException {
        super(DimmerController.class, label, device, builder);
        this.powerService = serviceFactory.newPowerService(device, this);
        this.dimmService = serviceFactory.newDimmService(device, this);
    }

    public void updatePower(final PowerType.Power.PowerState state) {
        data.getPowerStateBuilder().setState(state);
        notifyChange();
    }

    @Override
    public void setPower(final PowerType.Power.PowerState state) throws CouldNotPerformException {
        logger.debug("Setting [" + label + "] to Power [" + state.name() + "]");
        powerService.setPower(state);
    }

    @Override
    public PowerType.Power.PowerState getPower() throws CouldNotPerformException {
        return data.getPowerState().getState();
    }

    public void updateDimm(final Double dimm) {
        data.setValue(dimm);
        notifyChange();
    }

    @Override
    public void setDimm(Double dimm) throws CouldNotPerformException {
        dimmService.setDimm(dimm);
    }

    @Override
    public Double getDimm() throws CouldNotPerformException {
        return data.getValue();
    }
}
