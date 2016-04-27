/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.lib.layer.service.collection;

/*
 * #%L
 * DAL Library
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

import java.util.Collection;
import org.dc.bco.dal.lib.layer.service.PowerService;
import org.dc.jul.exception.CouldNotPerformException;
import rst.homeautomation.state.PowerStateType.PowerState;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public interface PowerStateOperationServiceCollection extends PowerService {

    @Override
    default public void setPower(final PowerState state) throws CouldNotPerformException {
        for (PowerService service : getPowerStateOperationServices()) {
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
    default public PowerState getPower() throws CouldNotPerformException {
        for (PowerService service : getPowerStateOperationServices()) {
            if (service.getPower().getValue() == PowerState.State.ON) {
                return PowerState.newBuilder().setValue(PowerState.State.ON).build();
            }
        }
        return PowerState.newBuilder().setValue(PowerState.State.OFF).build();
    }

    public Collection<PowerService> getPowerStateOperationServices();
}
