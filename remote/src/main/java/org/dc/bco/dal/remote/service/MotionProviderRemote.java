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

import org.dc.bco.dal.lib.layer.service.provider.MotionProvider;
import org.dc.jul.exception.CouldNotPerformException;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.state.MotionStateType.MotionState;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class MotionProviderRemote extends AbstractServiceRemote<MotionProvider> implements MotionProvider {

    public MotionProviderRemote() {
        super(ServiceType.MOTION_PROVIDER);
    }

    /**
     * Returns movement if at least one motion provider returns movement else no
     * movement.
     *
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public MotionState getMotion() throws CouldNotPerformException {
        for (MotionProvider provider : getServices()) {
            if (provider.getMotion().getValue() == MotionState.State.MOVEMENT) {
                return MotionState.newBuilder().setValue(MotionState.State.MOVEMENT).build();
            }
        }
        return MotionState.newBuilder().setValue(MotionState.State.NO_MOVEMENT).build();
    }
}
