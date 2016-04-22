/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.remote.service;

import org.dc.bco.dal.lib.layer.service.provider.SmokeStateProvider;
import org.dc.jul.exception.CouldNotPerformException;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.state.SmokeStateType.SmokeState;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class SmokeStateProviderRemote extends AbstractServiceRemote<SmokeStateProvider> implements SmokeStateProvider {

    public SmokeStateProviderRemote() {
        super(ServiceType.SMOKE_STATE_PROVIDER);
    }

    /**
     * If at least one smoke state provider returns smoke than that is returned.
     * Else if at least one returns some smoke than that is returned. Else no
     * smoke is returned.
     *
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public SmokeState getSmokeState() throws CouldNotPerformException {
        boolean someSmoke = false;
        for (SmokeStateProvider provider : getServices()) {
            if (provider.getSmokeState().getValue() == SmokeState.State.SMOKE) {
                return SmokeState.newBuilder().setValue(SmokeState.State.SMOKE).build();
            }
            if (provider.getSmokeState().getValue() == SmokeState.State.SOME_SMOKE) {
                someSmoke = true;
            }
        }
        if (someSmoke) {
            return SmokeState.newBuilder().setValue(SmokeState.State.SOME_SMOKE).build();
        } else {
            return SmokeState.newBuilder().setValue(SmokeState.State.NO_SMOKE).build();
        }
    }

}
