package org.openbase.bco.registry.user.core;

/*
 * #%L
 * REM UserRegistry Core
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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
import org.openbase.bco.registry.user.lib.jp.JPUserRegistryScope;
import org.openbase.jps.core.JPService;
import org.openbase.jps.preset.JPDebugMode;
import org.openbase.jps.preset.JPForce;
import org.openbase.jps.preset.JPReadOnly;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class UserRegistryLauncher {

    private static final Logger logger = LoggerFactory.getLogger(UserRegistryLauncher.class);

    public static final String USER_REGISTRY_NAME = UserRegistryLauncher.class.getSimpleName();

    private final UserRegistryController userRegistry;

    public UserRegistryLauncher() throws InitializationException, InterruptedException {
        try {
            this.userRegistry = new UserRegistryController();
            this.userRegistry.init();
            this.userRegistry.activate();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    public void shutdown() {
        if (userRegistry != null) {
            userRegistry.shutdown();
        }
    }

    public UserRegistryController getUserRegistry() {
        return userRegistry;
    }

    public static void main(String args[]) throws Throwable {
        logger.info("Start " + USER_REGISTRY_NAME + "...");

        /* Setup JPService */
        JPService.setApplicationName(USER_REGISTRY_NAME);

        JPService.registerProperty(JPUserRegistryScope.class);
        JPService.registerProperty(JPReadOnly.class);
        JPService.registerProperty(JPForce.class);
        JPService.registerProperty(JPDebugMode.class);

        JPService.parseAndExitOnError(args);

        UserRegistryLauncher userRegistry;
        try {
            userRegistry = new UserRegistryLauncher();
        } catch (InitializationException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
        }

        logger.info(USER_REGISTRY_NAME + " successfully started.");
    }
}
