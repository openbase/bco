package org.openbase.bco.registry.remote.session;

/*-
 * #%L
 * BCO Registry Remote
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

import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.authentication.lib.iface.BCOSession;
import org.openbase.bco.authentication.lib.jp.JPAuthentication;
import org.openbase.bco.authentication.lib.jp.JPBCOHomeDirectory;
import org.openbase.bco.registry.lib.jp.JPBCOAutoLoginUser;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.remote.login.BCOLogin;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.ExceptionProcessor;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.type.domotic.authentication.AuthTokenType.AuthToken;
import org.openbase.type.domotic.authentication.LoginCredentialsType.LoginCredentials;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class BCOSessionImpl implements BCOSession {

    private static final Logger LOGGER = LoggerFactory.getLogger(BCOSessionImpl.class);

    private final Properties loginProperties;
    private final SessionManager sessionManager;

    public BCOSessionImpl() {
        this(new Properties(), new SessionManager());
    }

    public BCOSessionImpl(final SessionManager sessionManager) {
        this(new Properties(), sessionManager);
    }

    public BCOSessionImpl(final Properties loginProperties) {
        this(loginProperties, SessionManager.getInstance());
    }

    public BCOSessionImpl(final Properties loginProperties, final SessionManager sessionManager) {
        this.loginProperties = loginProperties;
        this.sessionManager = sessionManager;
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     * @throws InterruptedException     {@inheritDoc}
     */
    @Override
    public void loginBCOUser() throws CouldNotPerformException, InterruptedException {

        // check if authentication is enabled.
        try {
            if (!JPService.getProperty(JPAuthentication.class).getValue()) {
                return;
            }
        } catch (JPNotAvailableException ex) {
            ExceptionPrinter.printHistory("Could not load " + JPAuthentication.class.getSimpleName(), ex, LOGGER, LogLevel.WARN);
        }

        final UnitConfig bcoUser = Registries.getUnitRegistry(true).getUnitConfigByAlias(UnitRegistry.BCO_USER_ALIAS);
        sessionManager.loginClient(bcoUser.getId(), true);
    }

    /**
     * {@inheritDoc}
     *
     * @param includeSystemUser {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<Void> autoLogin(final boolean includeSystemUser) {
        return GlobalCachedExecutorService.submit(() -> {
            try {
                try {
                    autoLoginDefaultUser(includeSystemUser);
                } catch (CouldNotPerformException ex) {
                    if (!includeSystemUser) {
                        throw ex;
                    }
                    loginBCOUser();
                }
            } catch (InterruptedException | CancellationException ex) {
                throw ex;
            } catch (Exception ex) {
                if(!ExceptionProcessor.isCausedBySystemShutdown(ex)) {
                    ExceptionPrinter.printHistoryAndReturnThrowable("Auto system login not possible. Please login via user interface to get system permissions!", ex, LOGGER, LogLevel.WARN);
                }
            }
            return null;
        });
    }

    public void autoLoginDefaultUser(final boolean includeSystemUser) throws CouldNotPerformException, InterruptedException {
        // check if authentication is enabled.
        try {
            if (!JPService.getProperty(JPAuthentication.class).getValue()) {
                return;
            }
        } catch (JPNotAvailableException ex) {
            ExceptionPrinter.printHistory("Could not load " + JPAuthentication.class.getSimpleName(), ex, LOGGER, LogLevel.WARN);
        }


        final String userId = loadAutoLoginUserId(includeSystemUser);

        // during tests the registry generation is skipped because the mock registry is handling the db initialization.
        if (!sessionManager.hasCredentialsForId(userId)) {
            String user = userId;
            try {
                // resolve user via registry name
                user = Registries.getUnitRegistry(false).getUnitConfigById(userId).getUserConfig().getUserName();
            } catch (Exception ex) {
                // user name can not be resolved.
            }
            throw new CouldNotPerformException("User[" + user + "] can not be used for auto login because its credentials are not stored in the local credential store.");
        }

        sessionManager.loginUser(userId, true);
        setLocalDefaultUser(userId);
    }

    private String loadAutoLoginUserId(final boolean includeSystemUser) throws CouldNotPerformException, InterruptedException {
        try {

            Registries.waitUntilReady();

            // load via local properties file
            String userId = loginProperties.getProperty(BCOLogin.DEFAULT_USER_KEY);

            // load via command line
            try {
                final String user = JPService.getProperty(JPBCOAutoLoginUser.class).getValue();
                if (!user.equals(JPBCOAutoLoginUser.OTHER)) {
                    userId = user;
                }
            } catch (JPNotAvailableException ex) {
                throw new CouldNotPerformException("Could not load " + JPBCOAutoLoginUser.class.getSimpleName(), ex);
            }

            // check if valid
            if (userId == null) {
                throw new NotAvailableException("AutoLoginUser");
            }

            try {
                // check if value is a valid user
                final UnitConfig unitConfigById = Registries.getUnitRegistry(true).getUnitConfigById(userId);
                if (unitConfigById.getUnitType() == UnitType.USER && (includeSystemUser || !unitConfigById.getUserConfig().getSystemUser())) {
                    return userId;
                }
            } catch (NotAvailableException ex) {
                // value not a valid user id
            }

            try {
                // check if value is valid user
                userId = Registries.getUnitRegistry().getUserUnitIdByUserName(userId);
                if (!includeSystemUser && Registries.getUnitRegistry().getUnitConfigById(userId).getUserConfig().getSystemUser()) {
                    throw new NotAvailableException("AutoLoginUser");
                }
                return userId;
            } catch (NotAvailableException ex) {
                // value not a valid username
                throw new CouldNotPerformException("Can not find a valid username or id in [" + userId + "].");
            }
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not lookup auto login user!", ex);
        }
    }


    /**
     * {@inheritDoc}
     *
     * @param userId {@inheritDoc}
     */
    @Override
    public void setLocalDefaultUser(final String userId) {
        loginProperties.setProperty(BCOLogin.DEFAULT_USER_KEY, userId);
        try {
            final File propertiesFile = new File(JPService.getProperty(JPBCOHomeDirectory.class).getValue(), BCOLogin.LOGIN_PROPERTIES);
            LOGGER.debug("Store Properties to " + propertiesFile.getAbsolutePath());
            if (!propertiesFile.exists()) {
                LOGGER.debug("Create: " + propertiesFile.createNewFile());
            }
            loginProperties.store(new FileOutputStream(propertiesFile), "BCO Login Properties");
        } catch (Exception ex) {
            ExceptionPrinter.printHistory("Could not store login properties!", ex, LOGGER);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param username     {@inheritDoc}
     * @param stayLoggedIn {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public void loginUserViaUsername(String username, boolean stayLoggedIn) throws CouldNotPerformException {
        loginUser(Registries.getUnitRegistry().getUserUnitIdByUserName(username), stayLoggedIn);
    }

    /**
     * {@inheritDoc}
     *
     * @param username     {@inheritDoc}
     * @param password     {@inheritDoc}
     * @param stayLoggedIn {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public void loginUserViaUsername(String username, String password, boolean stayLoggedIn) throws CouldNotPerformException {
        loginUser(Registries.getUnitRegistry().getUserUnitIdByUserName(username), password, stayLoggedIn);
    }

    /**
     * {@inheritDoc}
     *
     * @param username     {@inheritDoc}
     * @param passwordHash {@inheritDoc}
     * @param stayLoggedIn {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public void loginUserViaUsername(String username, byte[] passwordHash, boolean stayLoggedIn) throws CouldNotPerformException {
        loginUser(Registries.getUnitRegistry().getUserUnitIdByUserName(username), passwordHash, stayLoggedIn);
    }

    /**
     * {@inheritDoc}
     *
     * @param username     {@inheritDoc}
     * @param credentials  {@inheritDoc}
     * @param stayLoggedIn {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public void loginUserViaUsername(String username, LoginCredentials credentials, boolean stayLoggedIn) throws CouldNotPerformException {
        loginUser(Registries.getUnitRegistry().getUserUnitIdByUserName(username), credentials, stayLoggedIn);
    }

    /**
     * {@inheritDoc}
     *
     * @param id           {@inheritDoc}
     * @param stayLoggedIn {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public void loginUser(String id, boolean stayLoggedIn) throws CouldNotPerformException {
        sessionManager.loginUser(id, stayLoggedIn);
    }

    /**
     * {@inheritDoc}
     *
     * @param id           {@inheritDoc}
     * @param password     {@inheritDoc}
     * @param stayLoggedIn {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public void loginUser(String id, String password, boolean stayLoggedIn) throws CouldNotPerformException {
        sessionManager.loginUser(id, password, stayLoggedIn);
    }

    /**
     * {@inheritDoc}
     *
     * @param id           {@inheritDoc}
     * @param passwordHash {@inheritDoc}
     * @param stayLoggedIn {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public void loginUser(String id, byte[] passwordHash, boolean stayLoggedIn) throws CouldNotPerformException {
        sessionManager.loginUser(id, passwordHash, stayLoggedIn);
    }

    /**
     * {@inheritDoc}
     *
     * @param id           {@inheritDoc}
     * @param credentials  {@inheritDoc}
     * @param stayLoggedIn {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public void loginUser(String id, LoginCredentials credentials, boolean stayLoggedIn) throws CouldNotPerformException {
        sessionManager.loginUser(id, credentials, stayLoggedIn);
    }

    /**
     * {@inheritDoc}
     *
     * @param id           {@inheritDoc}
     * @param stayLoggedIn {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public void loginClient(String id, boolean stayLoggedIn) throws CouldNotPerformException {
        sessionManager.loginClient(id, stayLoggedIn);
    }

    /**
     * {@inheritDoc}
     *
     * @param id           {@inheritDoc}
     * @param password     {@inheritDoc}
     * @param stayLoggedIn {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public void loginClient(String id, String password, boolean stayLoggedIn) throws CouldNotPerformException {
        sessionManager.loginClient(id, password, stayLoggedIn);
    }

    /**
     * {@inheritDoc}
     *
     * @param id           {@inheritDoc}
     * @param credentials  {@inheritDoc}
     * @param stayLoggedIn {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public void loginClient(String id, LoginCredentials credentials, boolean stayLoggedIn) throws CouldNotPerformException {
        sessionManager.loginClient(id, credentials, stayLoggedIn);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void logout() {
        sessionManager.logout();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean isLoggedIn() {
        return sessionManager.isLoggedIn();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean isAdmin() {
        return sessionManager.isAdmin();
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public void reLogin() throws CouldNotPerformException {
        sessionManager.reLogin();
    }

    /**
     * {@inheritDoc}
     *
     * @param id               {@inheritDoc}
     * @param loginCredentials {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public void storeCredentials(String id, LoginCredentials loginCredentials) throws CouldNotPerformException {
        sessionManager.storeCredentials(id, loginCredentials);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     * @throws InterruptedException     {@inheritDoc}
     */
    @Override
    public AuthToken generateAuthToken() throws CouldNotPerformException, InterruptedException {
        return TokenGenerator.generateAuthToken(sessionManager);
    }

    /**
     * {@inheritDoc}
     *
     * @param timeout  {@inheritDoc}
     * @param timeUnit {@inheritDoc}
     *
     * @return {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     * @throws InterruptedException     {@inheritDoc}
     * @throws TimeoutException         {@inheritDoc}
     */
    @Override
    public AuthToken generateAuthToken(final long timeout, final TimeUnit timeUnit) throws CouldNotPerformException, InterruptedException, TimeoutException {
        return TokenGenerator.generateAuthToken(sessionManager, timeout, timeUnit);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public SessionManager getSessionManager() {
        return sessionManager;
    }
}
