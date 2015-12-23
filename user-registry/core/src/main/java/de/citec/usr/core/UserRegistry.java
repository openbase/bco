/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.usr.core;

import de.citec.jp.JPUserGroupConfigDatabaseDirectory;
import de.citec.jp.JPUserConfigDatabaseDirectory;
import de.citec.jp.JPUserRegistryScope;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.exception.InvalidStateException;
import de.citec.jul.exception.MultiException;
import de.citec.jul.exception.VerificationFailedException;
import de.citec.jul.exception.printer.ExceptionPrinter;
import de.citec.jul.storage.registry.jp.JPGitRegistryPlugin;
import de.citec.jul.storage.registry.jp.JPGitRegistryPluginRemoteURL;
import de.citec.jul.storage.registry.jp.JPInitializeDB;
import de.citec.usr.core.registry.UserRegistryService;
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

    private final UserRegistryService userRegistry;

    public UserRegistry() throws InitializationException, InterruptedException {
        try {
            this.userRegistry = new UserRegistryService();
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

    public UserRegistryService getUserRegistry() {
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
