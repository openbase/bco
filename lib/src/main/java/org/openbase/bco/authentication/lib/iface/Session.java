package org.openbase.bco.authentication.lib.iface;

/*-
 * #%L
 * BCO Authentication Library
 * %%
 * Copyright (C) 2017 - 2020 openbase.org
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

import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.type.domotic.authentication.LoginCredentialsType.LoginCredentials;

public interface Session {
    /**
     * Login a user. If a client is already logged in the user will be logged in on top of the client.
     * This method only works if the credentials for the user are stored by the session manager. To do this
     * have a look at {@link #storeCredentials(String, LoginCredentials)}.
     *
     * @param id           the id of the user to be logged in.
     * @param stayLoggedIn if the ticket of the user is automatically extended before it expires.
     *
     * @throws CouldNotPerformException if logging in fails.
     */
    void loginUser(String id, boolean stayLoggedIn) throws CouldNotPerformException;

    /**
     * Login a user with a password. The password is hashed and used for symmetric encryption.
     * If a client is already logged in the user will be logged in on top of the client.
     *
     * @param id           the id of the user to be logged in.
     * @param password     the password used as credentials.
     * @param stayLoggedIn if the ticket of the user is automatically extended before it expires.
     *
     * @throws CouldNotPerformException if logging in fails.
     */
    void loginUser(String id, String password, boolean stayLoggedIn) throws CouldNotPerformException;

    /**
     * Login a user. If a client is already logged in the user will be logged in on top of the client.
     *
     * @param id           the id of the user to be logged in.
     * @param credentials  the credentials of the user.
     * @param stayLoggedIn if the ticket of the user is automatically extended before it expires.
     *
     * @throws CouldNotPerformException if logging in fails.
     */
    void loginUser(String id, LoginCredentials credentials, boolean stayLoggedIn) throws CouldNotPerformException;

    /**
     * Login a client. If a user is already logged in the user will be logged out.
     * This method only works if the credentials for the client are stored by the session manager. To do this
     * have a look at {@link #storeCredentials(String, LoginCredentials)}.
     *
     * @param id           the id of the client to be logged in.
     * @param stayLoggedIn if the ticket of the client is automatically extended before it expires.
     *
     * @throws CouldNotPerformException if logging in fails.
     */
    void loginClient(String id, boolean stayLoggedIn) throws CouldNotPerformException;

    /**
     * Login a client with a password. The password is hashed and used for symmetric encryption.
     * If a user is already logged in the user will be logged out.
     *
     * @param id           the id of the client to be logged in.
     * @param password     the password used as credentials.
     * @param stayLoggedIn if the ticket of the client is automatically extended before it expires.
     *
     * @throws CouldNotPerformException if logging in fails.
     */
    void loginClient(String id, String password, boolean stayLoggedIn) throws CouldNotPerformException;

    /**
     * Login a client. If a user is already logged in the user will be logged out.
     *
     * @param id           the id of the client to be logged in.
     * @param credentials  the credentials of the client.
     * @param stayLoggedIn if the ticket of the client is automatically extended before it expires.
     *
     * @throws CouldNotPerformException if logging in fails.
     */
    void loginClient(String id, LoginCredentials credentials, boolean stayLoggedIn) throws CouldNotPerformException;

    /**
     * Logout by canceling the ticket renewal task and clearing the ticket and session key.
     * If a user is logged in his id will be cleared and if a client was also logged in the client will be logged in again.
     */
    void logout();

    /**
     * determines if a user is logged in.
     * does not validate ClientServerTicket and SessionKey
     *
     * @return Returns true if logged in otherwise false
     */
    boolean isLoggedIn();

    /**
     * Determines whether the currently logged in user is an admin.
     *
     * @return True if the user is an admin, false if not.
     */
    boolean isAdmin();

    /**
     * Logout and re-login what is possible while skipping notifications.
     *
     * @throws CouldNotPerformException if logging in again fails
     */
    void reLogin() throws CouldNotPerformException;

    /**
     * Add credentials to the session manager store. This will only succeed if the user/client
     * for which credentials are added is currently logged in.
     *
     * @param id               the id of the user/client for whom credentials are stored.
     * @param loginCredentials the credentials to be stored.
     */
    void storeCredentials(final String id, final LoginCredentials loginCredentials) throws CouldNotPerformException;

    /**
     * Method returns the session manager of this session.
     * @return the session manager instance used by this session.
     */
    SessionManager getSessionManager();
}
