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
import org.openbase.bco.authentication.core.AuthenticatorController;
import org.openbase.bco.authentication.lib.CredentialStore;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.dal.remote.DALRemote;
import org.openbase.bco.authentication.lib.BCO;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.remote.login.BCOLogin;
import org.openbase.jps.core.JPService;
import org.openbase.jps.preset.JPDebugMode;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Console;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class BCOConsole {

    public static final String APP_NAME = DALRemote.class.getSimpleName();

    private BCOConsole() throws CouldNotPerformException, InterruptedException {

        Console console = System.console();
        if (console == null) {
            System.out.println("Your terminal is not supported!");
            System.exit(55);
        }

        final SessionManager sessionManager = SessionManager.getInstance();
        System.out.println();
        try {
            while (!Thread.interrupted()) {
                try {
                    System.out.print("\rEstablish connection to bco   ");
                    Registries.waitForData(200, TimeUnit.MILLISECONDS);
                } catch (CouldNotPerformException ex) {
                    try {
                        System.out.print("\rEstablish connection to bco.  ");
                        Registries.waitForData(200, TimeUnit.MILLISECONDS);
                    } catch (CouldNotPerformException exx) {
                        try {
                            System.out.print("\rEstablish connection to bco.. ");
                            Registries.waitForData(200, TimeUnit.MILLISECONDS);
                        } catch (CouldNotPerformException exxx) {
                            try {
                                System.out.print("\rEstablish connection to bco...");
                                Registries.waitForData(200, TimeUnit.MILLISECONDS);
                            } catch (CouldNotPerformException exxxx) {
                                continue;
                            }
                        }
                    }
                }
                break;
            }

            System.out.print("\rWait for synchronization...                           ");
            Thread.sleep(100);
            Registries.waitUntilReady();
            System.out.println("\rBCO connection established. You are connected to " + LabelProcessor.getBestMatch(Registries.getUnitRegistry().getRootLocationConfig().getLabel(), "an unknown instance") + ".");
            System.out.println();

            loginLoop:
            while (!Thread.interrupted()) {

                if(!sessionManager.isLoggedIn()) {
                    System.out.println("Login required");
                    try {
                        sessionManager.login(Registries.getUnitRegistry().getUserUnitIdByUserName(console.readLine("user: ")), new String(console.readPassword("password: ")));
                    } catch (CouldNotPerformException ex) {
                        ExceptionPrinter.printHistory("Login not possible!", ex, System.err);
                        System.out.println("Please try again...");
                        System.out.println();
                        continue loginLoop;
                    }
                }

                System.out.println();
                System.out.println("available commands:");
                System.out.println();
                System.out.println("1: passwd - change the password of a given user");
                System.out.println("2: cloud connect - connect a user with the bco cloud service");
                System.out.println("3: cloud disconnect - disconnect a user from the bco cloud service");
                System.out.println("4: list user - prints a list of all users.");
                System.out.println("5: register user - creates a new user account");
                System.out.println("6: cloud update token - updates the cloud token");
                System.out.println("7: print key - prints the credential of a given user (admin only)");
                System.out.println();
                System.out.println("?: help - prints the command list");
                System.out.println("l: logout - logout the current user");
                System.out.println("q: exit, quit - close this console");
                System.out.println();

                boolean success = false;

                mainloop:
                while (!Thread.interrupted()) {

                    // validate if login is still valid
                    if (!sessionManager.isLoggedIn()) {
                        continue loginLoop;
                    }

                    System.out.print("please type a command or its number and press enter: ");
                    try {
                        final String command = console.readLine();
                        System.out.println();
                        switch (command) {
                            case "?":
                            case "help":
                                continue loginLoop;
                            case "quit":
                            case "exit":
                            case "q":
                                break loginLoop;
                            case "logout":
                            case "l":
                                sessionManager.logout();
                                continue loginLoop;
                            case "passwd":
                            case "1":

                                final String userId = Registries.getUnitRegistry().getUserUnitIdByUserName(console.readLine("user: "));

                                final String oldPwd;
                                // no old password verification needed for admins.
                                if (sessionManager.isAdmin()) {
                                    oldPwd = "";
                                } else {
                                    oldPwd = new String(console.readPassword("old password: "));
                                }

                                String newPwd = new String(console.readPassword("new password: "));
                                String newPwdConfirm = new String(console.readPassword("confirm new password:"));
                                System.out.println();

                                if (!newPwd.equals(newPwdConfirm)) {
                                    System.err.println("match failed!");
                                    continue;
                                }
                                sessionManager.changeCredentials(userId, oldPwd, newPwd);
                                break;
                            case "cloud connect":
                            case "2":
                                final CloudConnectorRemote cloudConnectorRemote = new CloudConnectorRemote();
                                System.out.println("For connecting your account with the bco cloud connector a new cloud user password is needed.");
                                System.out.println("You need this password for example again to pair the google cloud with the bco cloud service.");
                                System.out.println("Please choose a strong password for " +
                                        LabelProcessor.getBestMatch(Registries.getUnitRegistry().getUnitConfigById(sessionManager.getUserId()).getLabel(), "?")
                                        + " to protect the remote access of your home!");
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
                            case "list user":
                            case "4":
                                listUser(console);
                                break;
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
                                sessionManager.registerUser(Registries.getUnitRegistry().getUserUnitIdByUserName(newUser), newUserPwd, false);
                                break;
                            case "cloud update token":
                            case "6":
                                cloudUpdateToken(console);
                                break;
                            case "print key":
                            case "7":
                                printKey(console, sessionManager);
                                break;
                            default:
                                System.err.println("unknown command: " + command);
                                System.out.println();
                                continue;
                        }
                        System.out.println();
                    } catch (CouldNotPerformException | ExecutionException | TimeoutException ex) {
                        ExceptionPrinter.printHistory(ex, System.err);
                    }
                }
            }
            System.exit(0);
        } finally {
            System.out.println();
            sessionManager.logout();
        }
    }

    private void cloudDisconnect(final Console console) throws CouldNotPerformException, InterruptedException, TimeoutException, ExecutionException {
        final CloudConnectorRemote cloudConnectorRemote = new CloudConnectorRemote();
        cloudConnectorRemote.remove();
    }

    private void cloudUpdateToken(final Console console) throws CouldNotPerformException, InterruptedException, TimeoutException, ExecutionException {
        final CloudConnectorRemote cloudConnectorRemote = new CloudConnectorRemote();
        cloudConnectorRemote.setAuthorizationToken(cloudConnectorRemote.generateDefaultAuthorizationToken());
    }

    private void printKey(final Console console, final SessionManager session) throws CouldNotPerformException, InterruptedException, TimeoutException, ExecutionException {
        System.out.println();
        System.out.println("For security reasons, this command only requests and prints credentials from your local credential store.");
        System.out.println("Please type the user to look for.");
        String userId = Registries.getUnitRegistry().getUserUnitIdByUserName(console.readLine("user: "));
        System.out.println();

        boolean success = false;

        if (session.hasCredentialsForId(userId)) {
            System.out.println("client store key[" + session.getCredentialHashFromLocalStore(userId) + "]");
            success = true;
        }

        System.out.println();

        final CredentialStore store = new CredentialStore();
        store.init(AuthenticatorController.STORE_FILENAME);
        try {
            if (store.hasEntry(userId)) {
                System.out.println("server store key[" + store.getEntry(userId).getCredentials() + "]");
                success = true;
            }
        } finally {
            store.shutdown();
        }

        if(!success) {
            throw new CouldNotPerformException("no keys available!");
        }
    }

    private void listUser(final Console console) throws CouldNotPerformException, InterruptedException, TimeoutException, ExecutionException {
        final List<UnitConfig> userUnitConfigs = Registries.getUnitRegistry().getUnitConfigs(UnitType.USER);
        System.out.println("");
        System.out.println(userUnitConfigs.size()+" user available");
        System.out.println("");

        System.out.println();
        for (UnitConfig userUnitConfig : userUnitConfigs) {
            System.out.println(userUnitConfig.getUserConfig().getUserName());
        }
        System.out.println();
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
            ExceptionPrinter.printHistoryAndReturnThrowable(ex, LoggerFactory.getLogger(BCOConsole.class), LogLevel.ERROR);
        } catch (InterruptedException ex) {
            // just exit
            System.out.println();
            System.out.println("killed");
        }
    }
}
