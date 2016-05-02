/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.remote.service;

/*
 * #%L
 * DAL Remote
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import org.dc.bco.dal.lib.layer.service.provider.HandleProviderService;
import org.dc.jul.exception.CouldNotPerformException;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.state.HandleStateType.HandleState;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class HandleProviderRemote extends AbstractServiceRemote<HandleProviderService> implements HandleProviderService {

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
     * @throws java.lang.InterruptedException
     */
    @Override
    public HandleState getHandle() throws CouldNotPerformException, InterruptedException {
        boolean tilted = false;
        for (HandleProviderService provider : getServices()) {
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
