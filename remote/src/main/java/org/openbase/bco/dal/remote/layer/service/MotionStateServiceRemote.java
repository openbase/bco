package org.openbase.bco.dal.remote.layer.service;

/*
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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

import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.service.collection.MotionStateProviderServiceCollection;
import org.openbase.bco.dal.lib.layer.service.provider.MotionStateProviderService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.MotionStateType.MotionState;
import org.openbase.type.domotic.state.MotionStateType.MotionState.State;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class MotionStateServiceRemote extends AbstractServiceRemote<MotionStateProviderService, MotionState> implements MotionStateProviderServiceCollection {

    public MotionStateServiceRemote() {
        super(ServiceType.MOTION_STATE_SERVICE, MotionState.class);
    }

    /**
     * {@inheritDoc}
     * Computes the motion state as motion if at least one underlying services replies with motion and else no motion.
     * Additionally the last motion timestamp is set as the latest of the underlying services.
     *
     * @return {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected MotionState computeServiceState() throws CouldNotPerformException {
        return getMotionState(UnitType.UNKNOWN);
    }

    @Override
    public MotionState getMotionState(UnitType unitType) throws NotAvailableException {
        try {
            return (MotionState) generateAggregatedState(unitType, State.NO_MOTION, State.MOTION).build();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException(Services.getServiceStateName(getServiceType()), ex);
        }
    }
}
