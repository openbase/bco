/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.lib.layer.service.collection;

import java.util.Collection;
import org.dc.bco.dal.lib.layer.service.PowerService;
import org.dc.jul.exception.CouldNotPerformException;
import rst.homeautomation.state.PowerStateType.PowerState;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public interface PowerStateOperationServiceCollection extends PowerService {

    @Override
    default public void setPower(final PowerState state) throws CouldNotPerformException {
        for (PowerService service : getPowerStateOperationServices()) {
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
    default public PowerState getPower() throws CouldNotPerformException {
        for (PowerService service : getPowerStateOperationServices()) {
            if (service.getPower().getValue() == PowerState.State.ON) {
                return PowerState.newBuilder().setValue(PowerState.State.ON).build();
            }
        }
        return PowerState.newBuilder().setValue(PowerState.State.OFF).build();
    }

    public Collection<PowerService> getPowerStateOperationServices();
}
