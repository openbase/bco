package org.openbase.bco.dal.control.layer.unit.authorizationgroup;

/*-
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

import org.openbase.bco.dal.lib.layer.unit.authorizationgroup.AuthorizationGroup;
import org.openbase.bco.dal.lib.layer.unit.authorizationgroup.AuthorizationGroupController;
import org.openbase.bco.dal.lib.layer.unit.authorizationgroup.AuthorizationGroupControllerFactory;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

public class AuthorizationGroupControllerFactoryImpl implements AuthorizationGroupControllerFactory {

    protected final Logger logger = LoggerFactory.getLogger(AuthorizationGroupControllerFactoryImpl.class);
    private static AuthorizationGroupControllerFactoryImpl instance;

    public synchronized static AuthorizationGroupControllerFactory getInstance() {

        if (instance == null) {
            instance = new AuthorizationGroupControllerFactoryImpl();
        }
        return instance;
    }

    private AuthorizationGroupControllerFactoryImpl() {
    }

    @Override
    public AuthorizationGroupController newInstance(final UnitConfig config) throws InstantiationException {
        AuthorizationGroupController authorizationGroup;
        try {
            if (config == null) {
                throw new NotAvailableException("unit config");
            }
            authorizationGroup = new AuthorizationGroupControllerImpl();
            authorizationGroup.init(config);
        } catch (CouldNotPerformException | SecurityException | IllegalArgumentException | InterruptedException ex) {
            throw new InstantiationException(AuthorizationGroup.class, config.getId(), ex);
        }
        return authorizationGroup;
    }
}
