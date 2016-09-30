package org.openbase.bco.manager.user.remote;

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
import org.openbase.jul.extension.rsb.com.AbstractConfigurableRemote;
import org.openbase.bco.manager.user.lib.User;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.authorization.UserActivityType.UserActivity;
import rst.authorization.UserConfigType.UserConfig;
import rst.authorization.UserDataType.UserData;
import rst.authorization.UserPresenceStateType.UserPresenceState;
import rst.homeautomation.state.ActivationStateType.ActivationState;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class UserRemote extends AbstractConfigurableRemote<UserData, UnitConfig> implements User {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActivationState.getDefaultInstance()));
    }

    public UserRemote() {
        super(UserData.class, UnitConfig.class);
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
    public UserActivity getUserActivity() throws NotAvailableException {
        try {
            return getData().getUserActivity();
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
    public Future<Void> setUserActivity(UserActivity userActivity) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(userActivity, this, Void.class);
    }

    @Override
    public Future<Void> setUserPresenceState(UserPresenceState userPresenceState) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(userPresenceState, this, Void.class);
    }
}
