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
import org.openbase.bco.manager.user.lib.User;
import org.openbase.bco.manager.user.lib.UserController;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rsb.com.AbstractConfigurableController;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rsb.iface.RSBLocalServerInterface;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.authorization.UserActivityType;
import rst.authorization.UserConfigType.UserConfig;
import rst.authorization.UserDataType;
import rst.authorization.UserDataType.UserData;
import rst.authorization.UserPresenceStateType;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine
 * Threepwood</a>
 */
public class UserControllerImpl extends AbstractConfigurableController<UserDataType.UserData, UserDataType.UserData.Builder, UserConfig> implements UserController {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserData.getDefaultInstance()));
    }

    private boolean enabled;

    protected UserConfig config;

    public UserControllerImpl() throws org.openbase.jul.exception.InstantiationException {
        super(UserDataType.UserData.newBuilder());
    }

    @Override
    public void init(final UserConfig config) throws InitializationException, InterruptedException {
        this.config = config;
        logger.info("Initializing " + getClass().getSimpleName() + "[" + config.getId() + "] with scope [" + config.getScope().toString() + "]");
        super.init(config);
    }

    @Override
    public void registerMethods(final RSBLocalServerInterface server) throws CouldNotPerformException {
        RPCHelper.registerInterface(User.class, this, server);
    }

    @Override
    public String getId() throws NotAvailableException {
        return config.getId();
    }

    @Override
    public UserConfig getConfig() throws NotAvailableException {
        return config;
    }

    @Override
    public UserConfig applyConfigUpdate(UserConfig config) throws CouldNotPerformException, InterruptedException {
        setDataField(TYPE_FIELD_USER_NAME, config.getUserName());
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
            return config.getUserName();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("username", ex);
        }
    }

    @Override
    public UserActivityType.UserActivity getUserActivity() throws NotAvailableException {
        try {
            return getData().getUserActivity();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("user activity", ex);
        }
    }

    @Override
    public UserPresenceStateType.UserPresenceState getUserPresenceState() throws NotAvailableException {
        try {
            return getData().getUserPresenceState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("user presence state", ex);
        }
    }
}
