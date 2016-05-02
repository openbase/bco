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

import org.dc.bco.dal.lib.layer.service.provider.ReedSwitchProviderService;
import org.dc.jul.exception.CouldNotPerformException;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.state.ReedSwitchStateType.ReedSwitchState;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class ReedSwitchProviderRemote extends AbstractServiceRemote<ReedSwitchProviderService> implements ReedSwitchProviderService {

    public ReedSwitchProviderRemote() {
        super(ServiceType.REED_SWITCH_PROVIDER);
    }

    /**
     * Returns open if at least one of the reed switch providers returns open
     * and else no closed.
     *
     * @return
     * @throws CouldNotPerformException
     * @throws java.lang.InterruptedException
     */
    @Override
    public ReedSwitchState getReedSwitch() throws CouldNotPerformException, InterruptedException {
        for (ReedSwitchProviderService provider : getServices()) {
            if (provider.getReedSwitch().getValue() == ReedSwitchState.State.OPEN) {
                return ReedSwitchState.newBuilder().setValue(ReedSwitchState.State.OPEN).build();
            }
        }
        return ReedSwitchState.newBuilder().setValue(ReedSwitchState.State.CLOSED).build();
    }
}
