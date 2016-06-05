package org.dc.bco.manager.user.core;

/*
 * #%L
 * COMA UserManager Core
 * %%
 * Copyright (C) 2015 - 2016 DivineCooperation
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
