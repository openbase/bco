package org.openbase.bco.manager.user.core;

/*
 * #%L
 * COMA UserManager Core
 * %%
 * Copyright (C) 2015 - 2016 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
import java.util.concurrent.Future;
import org.openbase.bco.manager.user.lib.User;
import org.openbase.bco.manager.user.lib.UserController;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
import org.openbase.jul.extension.rsb.com.AbstractConfigurableController;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.authorization.UserActivityType.UserActivity;
import rst.authorization.UserDataType.UserData;
import rst.authorization.UserPresenceStateType.UserPresenceState;
import org.openbase.jul.extension.rsb.iface.RSBLocalServer;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 *
 */
public class UserControllerImpl extends AbstractConfigurableController<UserData, UserData.Builder, UnitConfig> implements UserController {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserData.getDefaultInstance()));
    }

    private boolean enabled;

    protected UnitConfig config;

    public UserControllerImpl() throws org.openbase.jul.exception.InstantiationException {
        super(UserData.newBuilder());
    }

    @Override
    public void init(final UnitConfig config) throws InitializationException, InterruptedException {
        this.config = config;
        logger.info("Initializing " + getClass().getSimpleName() + "[" + config.getId() + "] with scope [" + config.getScope().toString() + "]");
        super.init(config);
    }

    @Override
    public void registerMethods(final RSBLocalServer server) throws CouldNotPerformException {
        RPCHelper.registerInterface(User.class, this, server);
    }

    @Override
    public String getId() throws NotAvailableException {
        return config.getId();
    }

    @Override
    public UnitConfig getConfig() throws NotAvailableException {
        return config;
    }

    @Override
    public UnitConfig applyConfigUpdate(UnitConfig config) throws CouldNotPerformException, InterruptedException {
        setDataField(TYPE_FIELD_USER_NAME, config.getUserConfig().getUserName());
        return super.applyConfigUpdate(config);
    }

    @Override
    public void enable() throws CouldNotPerformException, InterruptedException {
        enabled = true;
        super.activate();
    }

    @Override
    public void disable() throws CouldNotPerformException, InterruptedException {
        enabled = false;
        super.deactivate();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getUserName() throws NotAvailableException {
        try {
            if (config == null) {
                throw new NotAvailableException("userconfig");
            }
            return config.getUserConfig().getUserName();
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
        try (ClosableDataBuilder<UserData.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setUserActivity(userActivity);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not set user activity to [" + userActivity + "] for " + this + "!", ex);
        }
        return null;
    }

    @Override
    public Future<Void> setUserPresenceState(UserPresenceState userPresenceState) throws CouldNotPerformException {
        try (ClosableDataBuilder<UserData.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setUserPresenceState(userPresenceState);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not set user presence state to [" + userPresenceState + "] for " + this + "!", ex);
        }
        return null;
    }
}
