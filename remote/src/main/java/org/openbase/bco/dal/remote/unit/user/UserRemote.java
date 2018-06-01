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
import java.util.concurrent.Future;
import org.openbase.bco.dal.lib.layer.unit.user.User;
import org.openbase.bco.dal.remote.unit.AbstractUnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rst.processing.ActionDescriptionProcessor;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.communicationpatterns.ResourceAllocationType.ResourceAllocation;
import rst.domotic.action.ActionAuthorityType.ActionAuthority;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.ActivationStateType.ActivationState;
import rst.domotic.state.ActivityStateType.ActivityState;
import rst.domotic.state.UserTransitStateType.UserTransitState;
import rst.domotic.unit.user.UserConfigType.UserConfig;
import rst.domotic.unit.user.UserDataType.UserData;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class UserRemote extends AbstractUnitRemote<UserData> implements User {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActivationState.getDefaultInstance()));
    }

    public UserRemote() {
        super(UserData.class);
    }

    @Override
    public ActivityState getActivityState() throws NotAvailableException {
        try {
            return getData().getActivityState();
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
    public Future<ActionFuture> setActivityState(ActivityState activityState) throws CouldNotPerformException {
        //TODO: services for Users have to registered, see dal issue 44
        ActionDescription.Builder actionDescription = ActionDescriptionProcessor.getActionDescription(ActionAuthority.getDefaultInstance(), ResourceAllocation.Initiator.SYSTEM);
        try {
            return applyAction(updateActionDescription(actionDescription, activityState, ServiceType.USER_ACTIVITY_STATE_SERVICE).build());
        } catch (InterruptedException ex) {
            throw new CouldNotPerformException("Interrupted while setting activationState.", ex);
        }
//        return RPCHelper.callRemoteMethod(activityState, this, Void.class);
    }

    @Override
    public Future<ActionFuture> setUserTransitState(UserTransitState userTransitState) throws CouldNotPerformException {
        //TODO: services for Users have to registered, see dal issue 44
        ActionDescription.Builder actionDescription = ActionDescriptionProcessor.getActionDescription(ActionAuthority.getDefaultInstance(), ResourceAllocation.Initiator.SYSTEM);
        try {
            return applyAction(updateActionDescription(actionDescription, userTransitState, ServiceType.USER_PRESENCE_STATE_SERVICE).build());
        } catch (InterruptedException ex) {
            throw new CouldNotPerformException("Interrupted while setting activationState.", ex);
        }
//        return RPCHelper.callRemoteMethod(userTransitState, this, Void.class);
    }
}
