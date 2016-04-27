/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.lib.layer.service.collection;

import java.util.Collection;
import org.dc.bco.dal.lib.layer.service.provider.TamperProvider;
import org.dc.jul.exception.CouldNotPerformException;
import rst.homeautomation.state.TamperStateType.TamperState;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public interface TamperStateProviderServiceCollection extends TamperProvider {

    /**
     * Returns tamper if at least one of the tamper providers returns tamper and
     * else no tamper.
     *
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    default public TamperState getTamper() throws CouldNotPerformException {
        for (TamperProvider provider : getTamperStateProviderServices()) {
            if (provider.getTamper().getValue() == TamperState.State.TAMPER) {
                return TamperState.newBuilder().setValue(TamperState.State.TAMPER).build();
            }
        }
        return TamperState.newBuilder().setValue(TamperState.State.NO_TAMPER).build();
    }

    public Collection<TamperProvider> getTamperStateProviderServices();
}
