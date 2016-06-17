package org.openbase.bco.manager.user.lib;

/*
 * #%L
 * COMA UserManager Library
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
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.iface.Identifiable;
import rst.authorization.UserActivityType.UserActivity;
import rst.authorization.UserPresenceStateType.UserPresenceState;

/**
 *
 * @author mpohling
 */
public interface User extends Identifiable<String> {

    public final static String TYPE_FIELD_USER_NAME = "user_name";

    public String getUserName() throws NotAvailableException;

    public UserActivity getUserActivity() throws NotAvailableException;

    public Future<Void> setUserActivity(UserActivity userActivity) throws CouldNotPerformException;

    public UserPresenceState getUserPresenceState() throws NotAvailableException;

    public Future<Void> setUserPresenceState(UserPresenceState userPresenceState) throws CouldNotPerformException;
}
