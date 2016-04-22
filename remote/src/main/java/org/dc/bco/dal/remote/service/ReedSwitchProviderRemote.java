/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.remote.service;

import org.dc.bco.dal.lib.layer.service.provider.ReedSwitchProvider;
import org.dc.jul.exception.CouldNotPerformException;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.state.ReedSwitchStateType.ReedSwitchState;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class ReedSwitchProviderRemote extends AbstractServiceRemote<ReedSwitchProvider> implements ReedSwitchProvider {

    public ReedSwitchProviderRemote() {
        super(ServiceType.REED_SWITCH_PROVIDER);
    }

    /**
     * Returns open if at least one of the reed switch providers returns open
     * and else no closed.
     *
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public ReedSwitchState getReedSwitch() throws CouldNotPerformException {
        for (ReedSwitchProvider provider : getServices()) {
            if (provider.getReedSwitch().getValue() == ReedSwitchState.State.OPEN) {
                return ReedSwitchState.newBuilder().setValue(ReedSwitchState.State.OPEN).build();
            }
        }
        return ReedSwitchState.newBuilder().setValue(ReedSwitchState.State.CLOSED).build();
    }
}
