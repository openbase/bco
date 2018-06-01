package org.openbase.bco.dal.lib.layer.unit.user;

/*
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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
import org.openbase.bco.dal.lib.layer.service.operation.ActivityStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.UserTransitStateOperationService;
import org.openbase.bco.dal.lib.layer.unit.BaseUnit;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import rst.domotic.unit.user.UserDataType.UserData;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface User extends BaseUnit<UserData>, ActivityStateOperationService, UserTransitStateOperationService {

    public final static String TYPE_FIELD_USER_NAME = "user_name";

    default public String getUserName() throws NotAvailableException {
        try {
            return getConfig().getUserConfig().getUserName();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("username", ex);
        }
    }

    default public String getName() throws NotAvailableException {
        try {
            return getConfig().getUserConfig().getFirstName() + " " + getConfig().getUserConfig().getLastName();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Name", ex);
        }
    }

    default public Boolean isAtHome() throws NotAvailableException {
        try {
            switch (getData().getUserTransitState().getValue()) {
                case AT_HOME:
                case SHORT_AT_HOME:
                case SOON_AWAY:
                    return true;
                case AWAY:
                case SHORT_AWAY:
                case SOON_AT_HOME:
                    return false;
                case UNKNOWN:
                    throw new InvalidStateException("UserTransitState unknown!");
                default:
                    throw new AssertionError("Type " + getData().getUserTransitState().getValue() + " not supported!");
            }
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("AtHomeState");
        }
    }
}
