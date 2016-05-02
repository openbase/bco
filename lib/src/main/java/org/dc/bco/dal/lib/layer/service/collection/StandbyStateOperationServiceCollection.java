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
import org.dc.bco.dal.lib.layer.service.StandbyService;
import org.dc.jul.exception.CouldNotPerformException;
import rst.homeautomation.state.StandbyStateType.StandbyState;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public interface StandbyStateOperationServiceCollection extends StandbyService {

    @Override
    default public void setStandby(StandbyState state) throws CouldNotPerformException {
        for (StandbyService service : getStandbyStateOperationServices()) {
            service.setStandby(state);
        }
    }

    /**
     * Returns running if at least one of the standby services is running and
     * else standby.
     *
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    default public StandbyState getStandby() throws CouldNotPerformException {
        for (StandbyService service : getStandbyStateOperationServices()) {
            if (service.getStandby().getValue() == StandbyState.State.RUNNING) {
                return StandbyState.newBuilder().setValue(StandbyState.State.RUNNING).build();
            }
        }
        return StandbyState.newBuilder().setValue(StandbyState.State.STANDBY).build();
    }

    public Collection<StandbyService> getStandbyStateOperationServices();
}
