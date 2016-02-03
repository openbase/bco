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

import org.dc.bco.dal.lib.layer.service.PowerService;
import org.dc.jul.exception.CouldNotPerformException;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.state.PowerStateType;
import rst.homeautomation.state.PowerStateType.PowerState;

/**
 *
 * @author mpohling
 */
public class PowerServiceRemote extends AbstractServiceRemote<PowerService> implements PowerService {

    public PowerServiceRemote() {
        super(ServiceType.POWER_SERVICE);
    }

    @Override
    public void setPower(final PowerStateType.PowerState.State state) throws CouldNotPerformException {
        for (PowerService service : getServices()) {
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
    public PowerStateType.PowerState getPower() throws CouldNotPerformException {
        for (PowerService service : getServices()) {
            if (service.getPower().getValue() == PowerState.State.ON) {
                return PowerStateType.PowerState.newBuilder().setValue(PowerState.State.ON).build();
            }
        }
        return PowerStateType.PowerState.newBuilder().setValue(PowerState.State.OFF).build();
    }
}
