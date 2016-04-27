/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.lib.layer.service.collection;

import java.util.Collection;
import org.dc.bco.dal.lib.layer.service.provider.SmokeStateProvider;
import org.dc.jul.exception.CouldNotPerformException;
import rst.homeautomation.state.SmokeStateType.SmokeState;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public interface SmokeStateProviderServiceCollection extends SmokeStateProvider {

    /**
     * If at least one smoke state provider returns smoke than that is returned.
     * Else if at least one returns some smoke than that is returned. Else no
     * smoke is returned.
     *
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    default public SmokeState getSmokeState() throws CouldNotPerformException {
        boolean someSmoke = false;
        for (SmokeStateProvider provider : getSmokeStateProviderServices()) {
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

    public Collection<SmokeStateProvider> getSmokeStateProviderServices();
}
