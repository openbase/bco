package org.openbase.bco.registry.remote.login;

/*-
 * #%L
 * BCO Registry Remote
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
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
import org.openbase.bco.authentication.lib.jp.JPAuthentication;
import org.openbase.bco.authentication.lib.jp.JPBCOHomeDirectory;
import org.openbase.bco.registry.lib.jp.JPBCOAutoLoginUser;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class BCOLogin {

    public static final String LOGIN_PROPERTIES = "login.properties";
    private static final Logger LOGGER = LoggerFactory.getLogger(BCOLogin.class);
    private static Properties loginProperties = new Properties();

    static {
        loadLoginProperties();
    }

    /**
     * Method resolves the credentials of the bco system user via the local credential store and initiates the login.
     *
     * @throws CouldNotPerformException is thrown if the auto login could not be performed, e.g. because the credentials are not available.
     * @throws InterruptedException     is thrown if the thread was externally interrupted.
     */
    public static void loginBCOUser() throws CouldNotPerformException, InterruptedException {

        // check if authentication is enabled.
        try {
            if (!JPService.getProperty(JPAuthentication.class).getValue()) {
                return;
            }
        } catch (JPNotAvailableException ex) {
            ExceptionPrinter.printHistory("Could not load " + JPAuthentication.class.getSimpleName(), ex, LOGGER, LogLevel.WARN);
        }

        final UnitConfig bcoUser = Registries.getUnitRegistry(true).getUnitConfigByAlias(UnitRegistry.BCO_USER_ALIAS);
        SessionManager.getInstance().loginClient(bcoUser.getId(), true);
    }

    /**
     * Method tries initiate the login of the auto login user. If this fails, the bco system user is used for the login.
     * If both are not available the task fails.
     *
     * @return a future representing the login task.
     */
    public static Future<Void> autoLogin(final boolean includeSystemUser) {
        return GlobalCachedExecutorService.submit(() -> {
            try {
                try {
                    BCOLogin.autoUserLogin(includeSystemUser);
                } catch (CouldNotPerformException ex) {
                    if(!includeSystemUser) {
                        throw ex;
                    }
                    BCOLogin.loginBCOUser();
                }
            } catch (InterruptedException | CancellationException ex) {
                throw ex;
            } catch (Exception ex) {
                ExceptionPrinter.printHistoryAndReturnThrowable("Auto system login not possible. Please login via user interface to get system permissions!", ex, LOGGER, LogLevel.WARN);
            }
            return null;
        });
    }

    /**
     * Method resolves the credentials of the configured auto login user via the local credential store and initiates the login.
     *
     * @throws CouldNotPerformException is thrown if the auto login could not be performed, e.g. because no auto login user was defined or the credentials are not available.
     * @throws InterruptedException     is thrown if the thread was externally interrupted.
     */
    public static void autoUserLogin(final boolean includeSystemUser) throws CouldNotPerformException, InterruptedException {
        // check if authentication is enabled.
        try {
            if (!JPService.getProperty(JPAuthentication.class).getValue()) {
                return;
            }
        } catch (JPNotAvailableException ex) {
            ExceptionPrinter.printHistory("Could not load " + JPAuthentication.class.getSimpleName(), ex, LOGGER, LogLevel.WARN);
        }


        final String userId = BCOLogin.loadAutoLoginUserId(includeSystemUser);

        // during tests the registry generation is skipped because the mock registry is handling the db initialization.
        if (!SessionManager.getInstance().hasCredentialsForId(userId)) {
            String user = userId;
            try {
                // resolve user via registry name
                user = Registries.getUnitRegistry(false).getUnitConfigById(userId).getUserConfig().getUserName();
            } catch (Exception ex) {
                // user name can not be resolved.
            }
            throw new CouldNotPerformException("User[" + user + "] can not be used for auto login because its credentials are not stored in the local credential store.");
        }

        SessionManager.getInstance().loginUser(userId, true);
        setLocalAutoLoginUser(userId);
    }

    private static String loadAutoLoginUserId(final boolean includeSystemUser) throws CouldNotPerformException, InterruptedException {
        try {

            Registries.waitUntilReady();

            // load via local properties file
            String userId = loginProperties.getProperty(DEFAULT_USER_KEY);

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
            if(userId ==null) {
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

    private static void loadLoginProperties() {
        try {
            final File propertiesFile = new File(JPService.getProperty(JPBCOHomeDirectory.class).getValue(), LOGIN_PROPERTIES);
            if (propertiesFile.exists()) {
                loginProperties.load(new FileInputStream(propertiesFile));
                LOGGER.debug("Load login properties from " + propertiesFile.getAbsolutePath());
            }
        } catch (Exception ex) {
            ExceptionPrinter.printHistory("No login properties found!", ex, LOGGER);
        }
    }

    public static final String DEFAULT_USER_KEY = "org.openbase.bco.default.user";

    /**
     * Sets the given user as new auto login user.
     * @param userId the id to identify the user account.
     */
    public static void setLocalAutoLoginUser(final String userId) {
        loginProperties.setProperty(DEFAULT_USER_KEY, userId);
        try {
            final File propertiesFile = new File(JPService.getProperty(JPBCOHomeDirectory.class).getValue(), LOGIN_PROPERTIES);
            LOGGER.debug("Store Properties to " + propertiesFile.getAbsolutePath());
            if (!propertiesFile.exists()) {
                LOGGER.debug("Create: " + propertiesFile.createNewFile());
            }
            loginProperties.store(new FileOutputStream(propertiesFile), "BCO Login Properties");
        } catch (Exception ex) {
            ExceptionPrinter.printHistory("Could not store login properties!", ex, LOGGER);
        }
    }

}
