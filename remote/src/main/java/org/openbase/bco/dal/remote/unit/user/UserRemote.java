package org.openbase.bco.dal.remote.unit.user;

/*
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import rst.domotic.state.UserActivityStateType.UserActivityState;
import rst.domotic.state.UserPresenceStateType.UserPresenceState;
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
    public UserActivityState getUserActivityState() throws NotAvailableException {
        try {
            return getData().getUserActivityState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("user activity", ex);
        }
    }

    @Override
    public UserPresenceState getUserPresenceState() throws NotAvailableException {
        try {
            return getData().getUserPresenceState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("user presence state", ex);
        }
    }

    @Override
    public Future<ActionFuture> setUserActivityState(UserActivityState userActivityState) throws CouldNotPerformException {
        //TODO: services for Users have to registered, see dal issue 44
        ActionDescription.Builder actionDescription = ActionDescriptionProcessor.getActionDescription(ActionAuthority.getDefaultInstance(), ResourceAllocation.Initiator.SYSTEM);
        try {
            return applyAction(updateActionDescription(actionDescription, userActivityState, ServiceType.USER_ACTIVITY_STATE_SERVICE).build());
        } catch (InterruptedException ex) {
            throw new CouldNotPerformException("Interrupted while setting activationState.", ex);
        }
//        return RPCHelper.callRemoteMethod(userActivityState, this, Void.class);
    }

    @Override
    public Future<ActionFuture> setUserPresenceState(UserPresenceState userPresenceState) throws CouldNotPerformException {
        //TODO: services for Users have to registered, see dal issue 44
        ActionDescription.Builder actionDescription = ActionDescriptionProcessor.getActionDescription(ActionAuthority.getDefaultInstance(), ResourceAllocation.Initiator.SYSTEM);
        try {
            return applyAction(updateActionDescription(actionDescription, userPresenceState, ServiceType.USER_PRESENCE_STATE_SERVICE).build());
        } catch (InterruptedException ex) {
            throw new CouldNotPerformException("Interrupted while setting activationState.", ex);
        }
//        return RPCHelper.callRemoteMethod(userPresenceState, this, Void.class);
    }
}
