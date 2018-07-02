package org.openbase.bco.dal.remote.unit.user;

/*
 * #%L
 * BCO DAL Remote
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

import java.util.List;
import java.util.concurrent.Future;

import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.layer.unit.user.User;
import org.openbase.bco.dal.remote.unit.AbstractUnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.NotSupportedException;
import org.openbase.jul.schedule.FutureProcessor;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.ActivationStateType.ActivationState;
import rst.domotic.state.ActivityStateType.ActivityState;
import rst.domotic.state.PresenceStateType.PresenceState;
import rst.domotic.state.UserTransitStateType.UserTransitState;
import rst.domotic.unit.user.UserConfigType.UserConfig;
import rst.domotic.unit.user.UserDataType.UserData;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class UserRemote extends AbstractUnitRemote<UserData> implements User {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActivationState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PresenceState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserTransitState.getDefaultInstance()));
    }

    public UserRemote() {
        super(UserData.class);
    }

    @Override
    public List<ActivityState> getActivityStateList() throws NotAvailableException {
        try {
            return getData().getActivityStateList();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("user activity", ex);
        }
    }

    @Override
    public UserTransitState getUserTransitState() throws NotAvailableException {
        try {
            return getData().getUserTransitState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("user presence state", ex);
        }
    }

    @Override
    public Future<ActionFuture> addActivityState(final ActivityState activityState) throws CouldNotPerformException {
        return applyAction(ActionDescriptionProcessor.generateActionDescriptionBuilderAndUpdate(activityState, ServiceType.MULTI_ACTIVITY_STATE_SERVICE,this));
    }

    @Override
    public Future<ActionFuture> setUserTransitState(UserTransitState userTransitState) throws CouldNotPerformException {
        return applyAction(ActionDescriptionProcessor.generateActionDescriptionBuilderAndUpdate(userTransitState, ServiceType.USER_TRANSIT_STATE_SERVICE, this));
    }

    @Override
    public Future<ActionFuture> setPresenceState(PresenceState presenceState) throws CouldNotPerformException {
        return applyAction(ActionDescriptionProcessor.generateActionDescriptionBuilderAndUpdate(presenceState, ServiceType.PRESENCE_STATE_SERVICE, this));
    }

    @Override
    public PresenceState getPresenceState() throws NotAvailableException {
        try {
            return getData().getPresenceState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("user presence state", ex);
        }
    }
}
