/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.unit;

import de.citec.dal.hal.device.DeviceInterface;
import de.citec.dal.hal.service.PowerService;
import de.citec.dal.hal.service.ServiceFactory;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.PowerPlugType;
import rst.homeautomation.PowerPlugType.PowerPlug;
import rst.homeautomation.states.PowerType;

/**
 *
 * @author mpohling
 */
public class PowerPlugController extends AbstractUnitController<PowerPlug, PowerPlug.Builder> implements PowerPlugInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerPlugType.PowerPlug.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerType.Power.getDefaultInstance()));
    }

    private final PowerService powerService;
    
    public PowerPlugController(final String label, DeviceInterface device, PowerPlug.Builder builder) throws InstantiationException {
        this(label, device, builder, device.getDefaultServiceFactory());
    }
    
    public PowerPlugController(final String label, DeviceInterface device, PowerPlug.Builder builder, final ServiceFactory serviceFactory) throws InstantiationException {
        super(PowerPlugController.class, label, device, builder);
        this.powerService = serviceFactory.newPowerService(device, this);
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
}
