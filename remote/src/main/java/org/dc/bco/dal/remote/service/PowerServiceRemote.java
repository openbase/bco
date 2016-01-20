/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.remote.service;

import org.dc.bco.dal.lib.layer.service.PowerService;
import org.dc.jul.exception.CouldNotPerformException;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.state.PowerStateType;
import rst.homeautomation.state.PowerStateType.PowerState;

/**
 *
 * @author mpohling
 */
public class PowerServiceRemote extends AbstractServiceRemote<PowerService> implements PowerService {

    public PowerServiceRemote() {
        super(ServiceType.POWER_SERVICE);
    }

    @Override
    public void setPower(final PowerStateType.PowerState.State state) throws CouldNotPerformException {
        for (PowerService service : getServices()) {
            service.setPower(state);
        }
    }

    /**
     * Returns on if at least one of the power services is on and else off.
     *
     * @return
     * @throws CouldNotPerformException
     */
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
