/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.user.core;

import org.dc.bco.registry.user.lib.jp.JPUserGroupConfigDatabaseDirectory;
import org.dc.bco.registry.user.lib.jp.JPUserConfigDatabaseDirectory;
import org.dc.bco.registry.user.lib.jp.JPUserRegistryScope;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InvalidStateException;
import org.dc.jul.exception.MultiException;
import org.dc.jul.exception.VerificationFailedException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.storage.registry.jp.JPGitRegistryPlugin;
import org.dc.jul.storage.registry.jp.JPGitRegistryPluginRemoteURL;
import org.dc.jul.storage.registry.jp.JPInitializeDB;
import org.dc.jps.core.JPService;
import org.dc.jps.preset.JPDebugMode;
import org.dc.jps.preset.JPForce;
import org.dc.jps.preset.JPReadOnly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mpohling
 */
public class UserRegistry {

    private static final Logger logger = LoggerFactory.getLogger(UserRegistry.class);

    public static final String USER_MANAGER_NAME = UserRegistry.class.getSimpleName();

    private final UserRegistryController userRegistry;

    public UserRegistry() throws InitializationException, InterruptedException {
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
        logger.info("Start " + USER_MANAGER_NAME + "...");

        /* Setup JPService */
        JPService.setApplicationName(USER_MANAGER_NAME);

        JPService.registerProperty(JPUserRegistryScope.class);
        JPService.registerProperty(JPUserConfigDatabaseDirectory.class);
        JPService.registerProperty(JPUserGroupConfigDatabaseDirectory.class);
        JPService.registerProperty(JPReadOnly.class);
        JPService.registerProperty(JPForce.class);
        JPService.registerProperty(JPDebugMode.class);
        JPService.registerProperty(JPInitializeDB.class);
        JPService.registerProperty(JPGitRegistryPlugin.class);
        JPService.registerProperty(JPGitRegistryPluginRemoteURL.class);

        JPService.parseAndExitOnError(args);

        UserRegistry userManager;
        try {
            userManager = new UserRegistry();
        } catch (InitializationException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
        }

        MultiException.ExceptionStack exceptionStack = null;

        if (!userManager.getUserRegistry().getUserRegistry().isConsistent()) {
            exceptionStack = MultiException.push(userManager, new VerificationFailedException("UserRegistry started in read only mode!", new InvalidStateException("Registry not consistent!")), exceptionStack);
        }

        if (!userManager.getUserRegistry().getGroupRegistry().isConsistent()) {
            exceptionStack = MultiException.push(userManager, new VerificationFailedException("GroupRegistry started in read only mode!", new InvalidStateException("Registry not consistent!")), exceptionStack);
        }

        try {
            MultiException.checkAndThrow(USER_MANAGER_NAME + " started in fallback mode!", exceptionStack);
        } catch (CouldNotPerformException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
        }
        logger.info(USER_MANAGER_NAME + " successfully started.");
    }
}
