package org.openbase.bco.app.util.launch;

/*
 * #%L
 * BCO App Utility
 * %%
 * Copyright (C) 2018 - 2021 openbase.org
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

import org.openbase.bco.app.util.launch.jp.JPExitOnError;
import org.openbase.bco.app.util.launch.jp.JPWaitForData;
import org.openbase.bco.authentication.lib.BCO;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jps.preset.JPDebugMode;
import org.openbase.jps.preset.JPVerbose;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.ExceptionProcessor;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.storage.registry.RegistryRemote;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class BCORegistryValidator extends BCOSystemValidator {

    public static void main(String[] args) {
        BCO.printLogo();
        JPService.setApplicationName("bco-registry-validate");
        JPService.registerProperty(JPDebugMode.class);
        JPService.registerProperty(JPVerbose.class);
        JPService.registerProperty(JPWaitForData.class);
        JPService.registerProperty(JPExitOnError.class);
        JPService.parseAndExitOnError(args);

        try {
            System.out.println("==================================================");
            System.out.println("BaseCubeOne - Registry Validator");
            System.out.println("==================================================");
            System.out.println();

            // prepare registries
            Registries.getRegistries(false);

            // skip validation if middleware is not ready
            if (!checkMiddleware(Registries.getTemplateRegistry(false))) {
                return;
            }

            // validate registry
            validateRegistries();

        } catch (ExitOnErrorException ex) {
            // just print errors via result print.
        } catch (InterruptedException | CancellationException ex) {
            System.exit(253);
            return;
        } catch (Throwable ex) {
            if (!ExceptionProcessor.isCausedBySystemShutdown(ex)) {
                ExceptionPrinter.printHistory(new CouldNotPerformException("Could not validate system!", ex), System.err);
                System.exit(254);
            }
            System.exit(251);
            return;
        }

        printResult();
    }

    public static void validateRegistries() throws CouldNotPerformException, InterruptedException {
        System.out.println("=== " + AnsiColor.colorize("Check registries" + (JPService.getValue(JPWaitForData.class, false) ? " and wait for data." : ""), AnsiColor.ANSI_BLUE) + " ===\n");

        // check
        final List<RegistryRemote> registries = Registries.getRegistries(JPService.getValue(JPWaitForData.class, false));
        for (final RegistryRemote registry : registries) {
            if (!check(registry, TimeUnit.SECONDS.toMillis(2))) {

                // in case we should wait
                if (JPService.getValue(JPWaitForData.class, false)) {
                    registry.waitUntilReady();
                }

                if (!registry.isReady()) {
                    System.out.println(StringProcessor.fillWithSpaces(registry.getName(), LABEL_RANGE, StringProcessor.Alignment.RIGHT) + "  " + AnsiColor.colorize(BUSY, AnsiColor.ANSI_YELLOW));
                }
                if (registry.isConsistent()) {
                    System.out.println(StringProcessor.fillWithSpaces(registry.getName(), LABEL_RANGE, StringProcessor.Alignment.RIGHT) + "  " + AnsiColor.colorize(OK, AnsiColor.ANSI_GREEN));
                } else {
                    System.out.println(StringProcessor.fillWithSpaces(registry.getName(), LABEL_RANGE, StringProcessor.Alignment.RIGHT) + "  " + AnsiColor.colorize("INCONSISTENT", AnsiColor.ANSI_RED));
                }
            }
        }
    }
}
