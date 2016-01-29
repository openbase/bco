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
import rst.authorization.UserConfigType.UserConfig;
import rst.authorization.UserDataType;
import rst.authorization.UserDataType.UserData;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 */
public class UserControllerImpl extends RSBCommunicationService<UserDataType.UserData, UserDataType.UserData.Builder> implements UserController {
    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserData.getDefaultInstance()));
    }

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
    public void activate() throws InterruptedException, CouldNotPerformException {
        super.activate();
    }

    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        super.deactivate();
    }

    @Override
    public String getId() throws NotAvailableException {
        return config.getId();
    }

    public UserConfig getConfig() throws NotAvailableException {
        return config;
    }

    public UserConfig updateConfig(UserConfig config) throws CouldNotPerformException {
        this.config = config;
        return config;
    }
}