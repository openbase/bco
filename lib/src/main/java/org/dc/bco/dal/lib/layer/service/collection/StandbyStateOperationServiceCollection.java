/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.lib.layer.service.collection;

import java.util.Collection;
import org.dc.bco.dal.lib.layer.service.StandbyService;
import org.dc.jul.exception.CouldNotPerformException;
import rst.homeautomation.state.StandbyStateType.StandbyState;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public interface StandbyStateOperationServiceCollection extends StandbyService {

    @Override
    default public void setStandby(StandbyState state) throws CouldNotPerformException {
        for (StandbyService service : getStandbyStateOperationServices()) {
            service.setStandby(state);
        }
    }

    /**
     * Returns running if at least one of the standby services is running and
     * else standby.
     *
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    default public StandbyState getStandby() throws CouldNotPerformException {
        for (StandbyService service : getStandbyStateOperationServices()) {
            if (service.getStandby().getValue() == StandbyState.State.RUNNING) {
                return StandbyState.newBuilder().setValue(StandbyState.State.RUNNING).build();
            }
        }
        return StandbyState.newBuilder().setValue(StandbyState.State.STANDBY).build();
    }

    public Collection<StandbyService> getStandbyStateOperationServices();
}
