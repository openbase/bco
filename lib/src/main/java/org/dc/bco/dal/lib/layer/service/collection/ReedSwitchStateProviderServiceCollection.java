/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.lib.layer.service.collection;

import java.util.Collection;
import org.dc.bco.dal.lib.layer.service.provider.ReedSwitchProvider;
import org.dc.jul.exception.CouldNotPerformException;
import rst.homeautomation.state.ReedSwitchStateType.ReedSwitchState;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public interface ReedSwitchStateProviderServiceCollection extends ReedSwitchProvider {

    /**
     * Returns open if at least one of the reed switch providers returns open
     * and else no closed.
     *
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    default public ReedSwitchState getReedSwitch() throws CouldNotPerformException {
        for (ReedSwitchProvider provider : getReedSwitchStateProviderServices()) {
            if (provider.getReedSwitch().getValue() == ReedSwitchState.State.OPEN) {
                return ReedSwitchState.newBuilder().setValue(ReedSwitchState.State.OPEN).build();
            }
        }
        return ReedSwitchState.newBuilder().setValue(ReedSwitchState.State.CLOSED).build();
    }

    public Collection<ReedSwitchProvider> getReedSwitchStateProviderServices();
}
