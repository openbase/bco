package org.openbase.bco.authentication.lib.iface;

import org.openbase.jul.exception.CouldNotPerformException;
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
     * Sets the given user as new auto login user.
     *
     * @param userId the id to identify the user account.
     */
    void setLocalAutoLoginUser(final String userId);

    /**
     * Method resolves the credentials of the configured auto login user via the local credential store and initiates the login.
     *
     * @throws CouldNotPerformException is thrown if the auto login could not be performed, e.g. because no auto login user was defined or the credentials are not available.
     * @throws InterruptedException     is thrown if the thread was externally interrupted.
     */
    void autoUserLogin(final boolean includeSystemUser) throws CouldNotPerformException, InterruptedException;

    /**
     * Method tries initiate the login of the auto login user. If this fails, the bco system user is used for the login.
     * If both are not available the task fails.
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
}
