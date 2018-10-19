package org.openbase.bco.dal.lib.layer.service.operation;

/*-
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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

import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.layer.service.provider.ActivityMultiStateProviderService;
import org.openbase.jul.annotation.RPCMethod;
import org.openbase.jul.exception.CouldNotPerformException;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.action.ActionParameterType.ActionParameter;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.ActivityMultiStateType.ActivityMultiState;

import java.util.ArrayList;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:pLeminoq@openbase.org">Tamino Huxohl</a>
 */
public interface ActivityMultiStateOperationService extends OperationService, ActivityMultiStateProviderService {

    @RPCMethod(legacy = true)
    default Future<ActionDescription> setActivityMultiState(final ActivityMultiState activityMultiState) throws CouldNotPerformException {
        return getServiceProvider().applyAction(ActionDescriptionProcessor.generateDefaultActionParameter(activityMultiState, ServiceType.ACTIVITY_MULTI_STATE_SERVICE));
    }

    default Future<ActionDescription> setActivityMultiState(final ActivityMultiState activityMultiState, final ActionParameter actionParameter) throws CouldNotPerformException {
        return getServiceProvider().applyAction(actionParameter.toBuilder().setServiceStateDescription(ActionDescriptionProcessor.generateServiceStateDescription(activityMultiState, ServiceType.ACTIVITY_MULTI_STATE_SERVICE)));
    }

    @RPCMethod(legacy = true)
    default Future<ActionDescription> removeActivityState(final String activityId) throws CouldNotPerformException {
        final ActivityMultiState.Builder activityMultiStateBuilder = getActivityMultiState().toBuilder();
        final ArrayList<String> activityIdList = new ArrayList<>(activityMultiStateBuilder.getActivityIdList());
        activityIdList.remove(activityId);
        activityMultiStateBuilder.clearActivityId();
        activityMultiStateBuilder.addAllActivityId(activityIdList);
        return setActivityMultiState(activityMultiStateBuilder.build());
    }

    @RPCMethod(legacy = true)
    default Future<ActionDescription> addActivityState(final String activityId) throws CouldNotPerformException {
        final ActivityMultiState.Builder activityMultiStateBuilder = getActivityMultiState().toBuilder();
        if (!getActivityMultiState().getActivityIdList().contains(activityId)) {
            final ArrayList<String> activityIdList = new ArrayList<>(activityMultiStateBuilder.getActivityIdList());
            activityIdList.add(activityId);
            activityMultiStateBuilder.clearActivityId();
            activityMultiStateBuilder.addAllActivityId(activityIdList);
        }
        return setActivityMultiState(activityMultiStateBuilder.build());
    }
}
