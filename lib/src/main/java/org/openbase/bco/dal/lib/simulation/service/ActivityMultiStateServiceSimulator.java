package org.openbase.bco.dal.lib.simulation.service;

/*-
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
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

import org.openbase.bco.dal.lib.layer.unit.UnitController;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.type.domotic.activity.ActivityConfigType.ActivityConfig;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.ActivityMultiStateType.ActivityMultiState;

/**
 * Custom service state simulator for activity multi state services.
 * This simulator creates an activity multi state for every activity config in the registry.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ActivityMultiStateServiceSimulator extends AbstractRandomServiceSimulator<ActivityMultiState> {

    /**
     * Create a new custom simulator for activity multi state services.
     *
     * @param unitController the controller for which the simulator is created.
     *
     * @throws InstantiationException if multi activity states could not be created from activity configs.
     */
    public ActivityMultiStateServiceSimulator(final UnitController unitController) throws InstantiationException {
        super(unitController, ServiceType.ACTIVITY_MULTI_STATE_SERVICE);
        try {
            detectAndRegisterServiceStates();
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    /**
     * Register a multi activity state for every activity config in the registry.
     *
     * @throws CouldNotPerformException if the activity config registry could not be accessed or if it does not contains
     *                                  any activity configs.
     */
    private void detectAndRegisterServiceStates() throws CouldNotPerformException {
        if (Registries.getActivityRegistry().getActivityConfigs().isEmpty()) {
            throw new NotAvailableException("activity configs");
        }

        for (final ActivityConfig activityConfig : Registries.getActivityRegistry().getActivityConfigs()) {
            registerServiceState(ActivityMultiState.newBuilder().addActivityId(activityConfig.getId()).build());
        }
    }
}
