package org.openbase.bco.dal.control.layer.unit.user;

/*
 * #%L
 * BCO DAL Control
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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
import org.openbase.bco.dal.lib.layer.unit.user.User;
import org.openbase.bco.dal.lib.layer.unit.user.UserController;
import org.openbase.bco.dal.lib.layer.unit.user.UserControllerFactory;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class UserControllerFactoryImpl implements UserControllerFactory {

    protected final Logger logger = LoggerFactory.getLogger(UserControllerFactoryImpl.class);
    private static UserControllerFactoryImpl instance;

    public synchronized static UserControllerFactory getInstance() {

        if (instance == null) {
            instance = new UserControllerFactoryImpl();
        }
        return instance;
    }

    private UserControllerFactoryImpl() {

    }

    @Override
    public UserController newInstance(final UnitConfig config) throws InstantiationException {
        UserController user;
        try {
            if (config == null) {
                throw new NotAvailableException("unitconfig");
            }
            logger.debug("Creating user [" + config.getUserConfig().getUserName() + "]");
            user = new UserControllerImpl();
            user.init(config);
        } catch (CouldNotPerformException | SecurityException | IllegalArgumentException | InterruptedException ex) {
            throw new InstantiationException(User.class, config.getId(), ex);
        }
        return user;
    }
}
