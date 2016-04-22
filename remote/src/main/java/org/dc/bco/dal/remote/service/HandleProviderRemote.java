/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.remote.service;

import org.dc.bco.dal.lib.layer.service.provider.HandleProvider;
import org.dc.jul.exception.CouldNotPerformException;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.state.HandleStateType.HandleState;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class HandleProviderRemote extends AbstractServiceRemote<HandleProvider> implements HandleProvider {

    public HandleProviderRemote() {
        super(ServiceType.HANDLE_PROVIDER);
    }

    /**
     * If at least one handle state provider returns open than that is returned.
     * Else if at least one returns tilted than that is returned. Else no closed
     * is returned.
     *
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public HandleState getHandle() throws CouldNotPerformException {
        boolean tilted = false;
        for (HandleProvider provider : getServices()) {
            if (provider.getHandle().getValue() == HandleState.State.OPEN) {
                return HandleState.newBuilder().setValue(HandleState.State.OPEN).build();
            }
            if (provider.getHandle().getValue() == HandleState.State.TILTED) {
                tilted = true;
            }
        }
        if (tilted) {
            return HandleState.newBuilder().setValue(HandleState.State.TILTED).build();
        }
        return HandleState.newBuilder().setValue(HandleState.State.CLOSED).build();
    }
}
