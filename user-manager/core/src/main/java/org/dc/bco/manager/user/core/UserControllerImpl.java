/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.user.core;

import org.dc.bco.manager.user.lib.User;
import org.dc.bco.manager.user.lib.UserController;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.rsb.com.RPCHelper;
import org.dc.jul.extension.rsb.com.RSBCommunicationService;
import org.dc.jul.extension.rsb.iface.RSBLocalServerInterface;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.authorization.UserActivityType;
import rst.authorization.UserConfigType.UserConfig;
import rst.authorization.UserDataType;
import rst.authorization.UserDataType.UserData;
import rst.authorization.UserPresenceStateType;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 */
public class UserControllerImpl extends RSBCommunicationService<UserDataType.UserData, UserDataType.UserData.Builder> implements UserController {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserData.getDefaultInstance()));
    }

    private boolean enabled;

    protected UserConfig config;

    public UserControllerImpl() throws org.dc.jul.exception.InstantiationException {
        super(UserDataType.UserData.newBuilder());
    }

    @Override
    public void init(final UserConfig config) throws InitializationException, InterruptedException {
        this.config = config;
        logger.info("Initializing " + getClass().getSimpleName() + "[" + config.getId() + "]");
        super.init(config.getScope());
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
    public UserConfig updateConfig(UserConfig config) throws CouldNotPerformException {
        this.config = config;
        return config;
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
