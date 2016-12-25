package org.openbase.bco.dal.remote.unit.user;

/*
 * #%L
 * COMA UserManager Remote
 * %%
 * Copyright (C) 2015 - 2016 openbase.org
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
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.state.UserActivityStateType.UserActivityState;
import rst.domotic.unit.user.UserConfigType.UserConfig;
import rst.domotic.unit.user.UserDataType.UserData;
import rst.domotic.state.UserPresenceStateType.UserPresenceState;
import rst.domotic.state.ActivationStateType.ActivationState;

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
    public String getUserName() throws NotAvailableException {
        try {
            return getConfig().getUserConfig().getUserName();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("username", ex);
        }
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
    public Future<Void> setUserActivityState(UserActivityState UserActivityState) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(UserActivityState, this, Void.class);
    }

    @Override
    public Future<Void> setUserPresenceState(UserPresenceState userPresenceState) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(userPresenceState, this, Void.class);
    }

    //TODO move into user unit interface
    public Boolean isAtHome() throws NotAvailableException {
        try {
            switch (getData().getUserPresenceState().getValue()) {
                case AT_HOME:
                case SHORT_AT_HOME:
                case SOON_AWAY:
                    return true;
                case AWAY:
                case SHORT_AWAY:
                case SOON_AT_HOME:
                    return false;
                case UNKNOWN:
                    throw new InvalidStateException("UserPresenceState is unknown!");
                default:
                    throw new AssertionError("Type " + getData().getUserPresenceState().getValue() + " not supported!");
            }
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("AtHomeState");
        }
    }

    //TODO move into user unit interface
    public String getName() throws NotAvailableException {
        try {
            return getConfig().getUserConfig().getFirstName() + " " + getConfig().getUserConfig().getLastName();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Name", ex);
        }
    }
}
