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

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.type.domotic.authentication.AuthTokenType.AuthToken;
import org.openbase.type.domotic.authentication.LoginCredentialsType.LoginCredentials;

import java.util.concurrent.Future;

public interface BCOSession extends Session {
    /**
     * Login a user. If a client is already logged in the user will be logged in on top of the client.
     * This method only works if the credentials for the user are stored by the session manager. To do this
     * have a look at {@link #storeCredentials(String, LoginCredentials)}.
     *
     * @param username     the username of the user to be logged in.
     * @param stayLoggedIn if the ticket of the user is automatically extended before it expires.
     *
     * @throws CouldNotPerformException if logging in fails.
     */
    void loginUserViaUsername(String username, boolean stayLoggedIn) throws CouldNotPerformException;

    /**
     * Login a user with a password. The password is hashed and used for symmetric encryption.
     * If a client is already logged in the user will be logged in on top of the client.
     *
     * @param username     the username of the user to be logged in.
     * @param password     the password used as credentials.
     * @param stayLoggedIn if the ticket of the user is automatically extended before it expires.
     *
     * @throws CouldNotPerformException if logging in fails.
     */
    void loginUserViaUsername(String username, String password, boolean stayLoggedIn) throws CouldNotPerformException;

    /**
     * Login a user with a password. The password is hashed and used for symmetric encryption.
     * If a client is already logged in the user will be logged in on top of the client.
     *
     * @param username     the username of the user to be logged in.
     * @param passwordHash the password hash used as credentials.
     * @param stayLoggedIn if the ticket of the user is automatically extended before it expires.
     *
     * @throws CouldNotPerformException if logging in fails.
     */
    void loginUserViaUsername(String username, byte[] passwordHash, boolean stayLoggedIn) throws CouldNotPerformException;

    /**
     * Login a user. If a client is already logged in the user will be logged in on top of the client.
     *
     * @param username     the username of the user to be logged in.
     * @param credentials  the credentials of the user.
     * @param stayLoggedIn if the ticket of the user is automatically extended before it expires.
     *
     * @throws CouldNotPerformException if logging in fails.
     */
    void loginUserViaUsername(String username, LoginCredentials credentials, boolean stayLoggedIn) throws CouldNotPerformException;

    /**
     * Sets the given user as new default user which can then be automatically logged in.
     *
     * @param userId the id to identify the user account.
     */
    void setLocalDefaultUser(final String userId);

    /**
     * Method resolves the credentials of the configured default login user via the local credential store and initiates the login.
     *
     * @throws CouldNotPerformException is thrown if the auto login could not be performed, e.g. because no auto login user was defined or the credentials are not available.
     * @throws InterruptedException     is thrown if the thread was externally interrupted.
     */
    void autoLoginDefaultUser(final boolean includeSystemUser) throws CouldNotPerformException, InterruptedException;

    /**
     * Method tries initiate the login of the auto login user. If this fails, the bco system user is used for the login if {@code includeSystemUser} is set to true.
     * If both are not available the task fails.
     *
     * @param includeSystemUser flag defines if the system user should be used as fallback if possible.
     *
     * @return a future representing the login task.
     */
    Future<Void> autoLogin(final boolean includeSystemUser);

    /**
     * Method resolves the credentials of the bco system user via the local credential store and initiates the login.
     *
     * @throws CouldNotPerformException is thrown if the auto login could not be performed, e.g. because the credentials are not available.
     * @throws InterruptedException     is thrown if the thread was externally interrupted.
     */
    void loginBCOUser() throws CouldNotPerformException, InterruptedException;

    /**
     * Method generates a new AuthToken including an authentication token for the user who is currently logged in.
     *
     * @return the token.
     *
     * @throws CouldNotPerformException is thrown if the token could not be generated.
     * @throws InterruptedException     is thrown if the thread was externally interrupted which could indicated an system shutdown.
     */
    AuthToken generateAuthToken() throws CouldNotPerformException, InterruptedException;
}
