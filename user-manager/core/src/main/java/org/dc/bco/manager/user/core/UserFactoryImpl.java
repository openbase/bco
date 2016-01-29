/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.user.core;

import org.dc.bco.manager.user.lib.User;
import org.dc.bco.manager.user.lib.UserController;
import org.dc.bco.manager.user.lib.UserFactory;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.NotAvailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.authorization.UserConfigType.UserConfig;

/**
 *
 * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public class UserFactoryImpl implements UserFactory {

    protected final Logger logger = LoggerFactory.getLogger(UserFactoryImpl.class);
    private static UserFactoryImpl instance;

    public synchronized static UserFactory getInstance() {

        if (instance == null) {
            instance = new UserFactoryImpl();
        }
        return instance;
    }

    private UserFactoryImpl() {

    }

    @Override
    public UserController newInstance(final UserConfig config) throws InstantiationException {
        UserController user;
        try {
            if (config == null) {
                throw new NotAvailableException("userconfig");
            }
            logger.info("Creating user [" + config.getUserName()  + "]");
            user = new UserControllerImpl();
            user.init(config);
        } catch (CouldNotPerformException | SecurityException | IllegalArgumentException | InterruptedException ex) {
            throw new InstantiationException(User.class, config.getId(), ex);
        }
        return user;
    }
}
