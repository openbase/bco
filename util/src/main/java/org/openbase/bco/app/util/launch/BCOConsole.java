package org.openbase.bco.app.util.launch;

/*-
 * #%L
 * BCO App Utility
 * %%
 * Copyright (C) 2018 - 2019 openbase.org
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

import org.openbase.bco.app.cloudconnector.CloudConnectorRemote;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.dal.remote.DALRemote;
import org.openbase.bco.authentication.lib.BCO;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jps.preset.JPDebugMode;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Console;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class BCOConsole {

    public static final String APP_NAME = DALRemote.class.getSimpleName();
    private static final Logger LOGGER = LoggerFactory.getLogger(DALRemote.class);


    private BCOConsole() throws CouldNotPerformException, InterruptedException {

        Console console = System.console();
        if (console == null) {
            System.out.println("Couldn't get Console instance");
            System.exit(0);
        }

        System.out.println();
        System.out.println("Welcome to the bco console, connect to bco... ");
        Registries.waitForData();
        Registries.waitUntilReady();
        System.out.println("connected");
        System.out.println();

        while (Thread.currentThread().isInterrupted()) {
            System.out.println("Login required");
            try {
                SessionManager.getInstance().login(Registries.getUnitRegistry().getUserUnitIdByUserName(console.readLine("user: ")), new String(console.readPassword("password: ")));
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory("Login not possible!", ex, LOGGER);
            }
            System.out.println("Please try again...");
            System.out.println();
        }
        System.out.println();
        System.out.println("available commands:");
        System.out.println();
        System.out.println("1: passwd - change the password of a given user");
        System.out.println("2: cloud connect - connect a user with the bco cloud service");
        System.out.println("3: cloud disconnect - disconnect a user from the bco cloud service");
        System.out.println("4: exit, quit, logout - close this console");
        System.out.println("5: register user - creates a new user account");
        System.out.println("6: cloud update token - updates the cloud token");
        System.out.println();
        System.out.println();

        mainloop:
        while (!Thread.interrupted()) {
            System.out.println("please type a command or its number and press enter...");
            try {
                final String command = console.readLine();
                switch (command) {
                    case "logout":
                    case "quit":
                    case "exit":
                    case "4":
                        SessionManager.getInstance().logout();
                        break mainloop;
                    case "register user":
                    case "5":
                        String newUser = console.readLine("user: ");
                        String newUserPwd = new String(console.readPassword("new password: "));
                        String newUserPwdConfirm = new String(console.readPassword("confirm new password: "));
                        System.out.println();
                        if (!newUserPwd.equals(newUserPwdConfirm)) {
                            System.err.println("match failed!");
                            continue;
                        }
                        SessionManager.getInstance().registerUser(Registries.getUnitRegistry().getUserUnitIdByUserName(newUser), newUserPwd,false);
                        break;
                    case "passwd":
                    case "1":
                        String user = console.readLine("user:");
                        String oldPwd = new String(console.readPassword("old password: "));
                        String newPwd = new String(console.readPassword("new password: "));
                        String newPwdConfirm = new String(console.readPassword("confirm new password:"));
                        System.out.println();

                        if (!newPwd.equals(newPwdConfirm)) {
                            System.err.println("match failed!");
                            continue;
                        }
                        SessionManager.getInstance().changeCredentials(Registries.getUnitRegistry().getUserUnitIdByUserName(user), oldPwd, newPwd);
                        break;
                    case "cloud connect":
                    case "2":
                        final CloudConnectorRemote cloudConnectorRemote = new CloudConnectorRemote();
                        System.out.println("For connecting your accound with the bco cloud connector a new cloud user password is needed.");
                        System.out.println("You need this password for example again to pair the google cloud with the bco cloud service.");
                        System.out.println("Please choose a strong password to protect the remote access of your home!");
                        String cloudPwd = new String(console.readPassword("your new cloud password:"));
                        String cloudPwdConfirm = new String(console.readPassword("confirm your new cloud password:"));
                        System.out.println();

                        if (!cloudPwd.equals(cloudPwdConfirm)) {
                            throw new InvalidStateException("match failed!");
                        }
                        cloudConnectorRemote.register(cloudPwd).get(1, TimeUnit.MINUTES);
                        break;
                    case "cloud disconnect":
                    case "3":
                        cloudDisconnect(console);
                        break;
                    case "cloud update token":
                    case "6":
                        cloudUpdateToken(console);
                        break;
                    default:
                        System.err.println("unknown command: "+ command);
                        System.out.println();
                        continue;
                }
                System.out.println("successful");
                System.out.println();
            } catch (CouldNotPerformException | ExecutionException | TimeoutException ex) {
                ExceptionPrinter.printHistory(ex, LOGGER);
            }
        }
        System.exit(0);
    }

    private void cloudDisconnect(Console console) throws CouldNotPerformException, InterruptedException, TimeoutException, ExecutionException {
        final CloudConnectorRemote cloudConnectorRemote = new CloudConnectorRemote();
        cloudConnectorRemote.remove();
    }

    private void cloudUpdateToken(Console console) throws CouldNotPerformException, InterruptedException, TimeoutException, ExecutionException {
        final CloudConnectorRemote cloudConnectorRemote = new CloudConnectorRemote();
        cloudConnectorRemote.setAuthorizationToken(cloudConnectorRemote.generateDefaultAuthorizationToken());
    }


    public static void main(String[] args) {
        BCO.printLogo();

        /* Setup JPService */
        JPService.setApplicationName(APP_NAME);
        JPService.registerProperty(JPDebugMode.class);
        JPService.parseAndExitOnError(args);

        try {
            new BCOConsole();
        } catch (CouldNotPerformException ex) {
            // just exit
        } catch (InterruptedException ex) {
            ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER, LogLevel.ERROR);
        }
    }
}
