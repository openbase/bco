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

import com.google.protobuf.ProtocolStringList;
import org.openbase.bco.dal.lib.layer.service.provider.ActivityStateProviderService;
import org.openbase.bco.dal.lib.layer.service.provider.MultiActivityStateProviderService;
import org.openbase.jul.annotation.RPCMethod;
import org.openbase.jul.exception.CouldNotPerformException;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.state.ActivityMultiStateType.ActivityMultiState;
import rst.domotic.state.ActivityStateType.ActivityState;
import rst.domotic.unit.user.UserDataType.UserData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

/**
 *
 * @author <a href="mailto:pLeminoq@openbase.org">Tamino Huxohl</a>
 */
public interface MultiActivityStateOperationService extends OperationService, MultiActivityStateProviderService {

    @RPCMethod
    Future<ActionFuture> setActivityMultiState(final ActivityMultiState activityMultiState) throws CouldNotPerformException;

    @RPCMethod
    default Future<ActionFuture> removeActivityState(final ActivityState activityState) throws CouldNotPerformException{
        final ActivityMultiState.Builder activityMultiStateBuilder = getActivityMultiState().toBuilder();
        final ArrayList<String> activityIdList = new ArrayList<>(activityMultiStateBuilder.getActivityIdList());
        activityIdList.remove(activityState);
        activityMultiStateBuilder.clearActivityId();
        activityMultiStateBuilder.addAllActivityId(activityIdList);
        return setActivityMultiState(activityMultiStateBuilder.build());
    }

    @RPCMethod
    default Future<ActionFuture> addActivityState(final ActivityState activityState) throws CouldNotPerformException {
        final ActivityMultiState.Builder activityMultiStateBuilder = getActivityMultiState().toBuilder();
        final ArrayList<String> activityIdList = new ArrayList<>(activityMultiStateBuilder.getActivityIdList());
        activityIdList.remove(activityState);
        activityMultiStateBuilder.clearActivityId();
        activityMultiStateBuilder.addAllActivityId(activityIdList);
        return setActivityMultiState(activityMultiStateBuilder.build());
    }
}
