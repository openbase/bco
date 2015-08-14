/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.service;

import de.citec.dal.hal.service.PowerService;
import de.citec.jul.exception.CouldNotPerformException;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.state.PowerStateType;
import rst.homeautomation.state.PowerStateType.PowerState;

/**
 *
 * @author mpohling
 */
public class PowerServiceRemote extends AbstractServiceRemote<PowerService> implements PowerService {

    public PowerServiceRemote() {
        super(ServiceType.POWER_PROVIDER);
    }

    @Override
    public void setPower(final PowerStateType.PowerState.State state) throws CouldNotPerformException {
        for (PowerService service : getServices()) {
            service.setPower(state);
        }
    }

    @Override
    public PowerStateType.PowerState getPower() throws CouldNotPerformException {
        for (PowerService service : getServices()) {
            if (service.getPower().getValue() == PowerState.State.ON) {
                return PowerStateType.PowerState.newBuilder().setValue(PowerState.State.ON).build();
            }
        }
        return PowerStateType.PowerState.newBuilder().setValue(PowerState.State.OFF).build();
    }
}
