package org.openbase.bco.dal.remote.layer.unit.user;

/*
 * #%L
 * BCO DAL Remote
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

import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.layer.unit.user.User;
import org.openbase.bco.dal.remote.layer.unit.AbstractUnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.schedule.FutureProcessor;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.ActivityMultiStateType.ActivityMultiState;
import org.openbase.type.domotic.state.GlobalPositionStateType.GlobalPositionState;
import org.openbase.type.domotic.state.LocalPositionStateType.LocalPositionState;
import org.openbase.type.domotic.state.PresenceStateType.PresenceState;
import org.openbase.type.domotic.state.UserTransitStateType.UserTransitState;
import org.openbase.type.domotic.unit.user.UserDataType.UserData;

import java.util.concurrent.Future;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class UserRemote extends AbstractUnitRemote<UserData> implements User {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PresenceState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActivityMultiState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserTransitState.getDefaultInstance()));
    }

    public UserRemote() {
        super(UserData.class);
    }

    @Override
    public Future<ActionDescription> setActivityMultiState(final ActivityMultiState activityMultiState) {
        try {
            return applyAction(ActionDescriptionProcessor.generateDefaultActionParameter(activityMultiState, ServiceType.ACTIVITY_MULTI_STATE_SERVICE, this));
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, ex);
        }
    }

    @Override
    public Future<ActionDescription> setUserTransitState(final UserTransitState userTransitState) {
        try {
            return applyAction(ActionDescriptionProcessor.generateDefaultActionParameter(userTransitState, ServiceType.USER_TRANSIT_STATE_SERVICE, this));
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, ex);
        }
    }

    @Override
    public Future<ActionDescription> setPresenceState(final PresenceState presenceState) {
        try {
            return applyAction(ActionDescriptionProcessor.generateDefaultActionParameter(presenceState, ServiceType.PRESENCE_STATE_SERVICE, this));
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, ex);
        }
    }

    @Override
    public Future<ActionDescription> setGlobalPositionState(final GlobalPositionState globalPositionState) {
        try {
            return applyAction(ActionDescriptionProcessor.generateDefaultActionParameter(globalPositionState, ServiceType.GLOBAL_POSITION_STATE_SERVICE, this));
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, ex);
        }
    }

    @Override
    public Future<ActionDescription> setLocalPositionState(final LocalPositionState localPositionState) {
        try {
            return applyAction(ActionDescriptionProcessor.generateDefaultActionParameter(localPositionState, ServiceType.LOCAL_POSITION_STATE_SERVICE, this));
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, ex);
        }
    }
}
