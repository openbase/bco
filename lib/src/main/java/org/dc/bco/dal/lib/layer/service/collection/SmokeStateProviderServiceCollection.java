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
