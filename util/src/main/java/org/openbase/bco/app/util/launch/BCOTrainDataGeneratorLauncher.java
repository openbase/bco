package org.openbase.bco.app.util.launch;

/*
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

import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.jp.JPProviderControlMode;
import org.openbase.bco.dal.remote.action.Actions;
import org.openbase.bco.dal.remote.layer.unit.ColorableLightRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.remote.layer.unit.location.LocationRemote;
import org.openbase.bco.authentication.lib.BCO;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.remote.login.BCOLogin;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.PowerStateType.PowerState;
import org.openbase.type.domotic.state.PresenceStateType.PresenceState;
import org.openbase.type.domotic.state.PresenceStateType.PresenceState.State;
import org.openbase.type.vision.HSBColorType.HSBColor;

import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class BCOTrainDataGeneratorLauncher {

    public static final Logger LOGGER = LoggerFactory.getLogger(BCOTrainDataGeneratorLauncher.class);
    private static final Random random = new Random(System.currentTimeMillis());

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        BCO.printLogo();
        JPService.registerProperty(JPProviderControlMode.class, true);

        BCOLogin.getSession().autoLogin(true);

        try {
            LOGGER.info("please make sure bco is started with the --provider-control flag, otherwise no provider services can be synthesised.");
            LOGGER.info("waiting for registry synchronization...");
            Registries.waitUntilReady();

            LOGGER.info("init simulation setup");

            // init
            final int trainingSetCounter = 10;
            int conditionCounter = 0;
            LocationRemote location = Units.getUnit(Registries.getUnitRegistry(true).getUnitConfigByAlias("Location-8"), true, Units.LOCATION);
            ColorableLightRemote light = Units.getUnit(Registries.getUnitRegistry(true).getUnitConfigByAlias("ColorableLight-15"), true, Units.COLORABLE_LIGHT);
            final ActionDescription absentState = ActionDescriptionProcessor.generateActionDescriptionBuilder(
                    PresenceState.newBuilder().setValue(State.ABSENT).build(),
                    ServiceType.PRESENCE_STATE_SERVICE,
                    location).build();
            final ActionDescription presentState = ActionDescriptionProcessor.generateActionDescriptionBuilder(
                    PresenceState.newBuilder().setValue(State.PRESENT).build(),
                    ServiceType.PRESENCE_STATE_SERVICE,
                    location).build();
            //
            location.setColor(HSBColor.newBuilder().setBrightness(1).setSaturation(0).build());
            boolean present = false;


            LOGGER.info("generate "+ trainingSetCounter + " training sets.");

            while (!Thread.interrupted() && conditionCounter <= trainingSetCounter) {

                LOGGER.info("=== generate condition {} for training set {} ===", (present ? "PRESENT" : "ABSENT"), (conditionCounter + 1));

                try {
                    // check condition
                    if (present) {

                        // random order
                        if (random.nextBoolean()) {
                            // human leaving the room
                            Actions.waitForExecution(location.applyAction(absentState));
                            // and after a while
                            waitBetweenActions();
                            // they is switching the light off.
//                            Actions.waitForExecution(location.setPowerState(PowerState.State.OFF, UnitType.LIGHT));
                            Actions.waitForExecution(light.setPowerState(PowerState.State.OFF));
                        } else {

                            // human is switching the light off.
//                            Actions.waitForExecution(location.setPowerState(PowerState.State.OFF, UnitType.LIGHT));
                            Actions.waitForExecution(light.setPowerState(PowerState.State.OFF));
                            // and after a while
                            waitBetweenActions();
                            // they is leaving the room
                            Actions.waitForExecution(location.applyAction(absentState));
                        }
                        conditionCounter++;
                    } else {
                        // random order
                        if (random.nextBoolean()) {
                            // human entering the room
                            Actions.waitForExecution(location.applyAction(presentState));
                            // and after a while
                            waitBetweenActions();
                            // they is switching the light on.
//                            Actions.waitForExecution(location.setPowerState(PowerState.State.ON, UnitType.LIGHT));
                            Actions.waitForExecution(light.setPowerState(PowerState.State.ON));
                        } else {
                            // human is switching the light on
//                            Actions.waitForExecution(location.setPowerState(PowerState.State.ON, UnitType.LIGHT));
                            Actions.waitForExecution(light.setPowerState(PowerState.State.ON));
                            // and after a while
                            waitBetweenActions();
                            // they is entering the room
                            Actions.waitForExecution(location.applyAction(presentState));
                        }
                    }
                } catch (CancellationException ex) {
                    ExceptionPrinter.printHistory("generator run skipped!", ex, LOGGER);
                }
                // toggle condition
                present = !present;
                waitUntilNextAction();
            }
            LOGGER.info("generate finished.");
        } catch (InterruptedException e) {
            LOGGER.info("generate canceled by user.");
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Training data generation failed!", ex, LOGGER);
            System.exit(1);
        }
        System.exit(0);
    }

    private static void waitBetweenActions() throws InterruptedException {
        TimeUnit timeUnit = TimeUnit.MILLISECONDS;
//        long staticDelay = 1000;
        long staticDelay = 10;
//        int maxRandomOffset = 5000;
        int maxRandomOffset = 100;

        long delay = staticDelay + random.nextInt(maxRandomOffset);
        LOGGER.info("wait {} sec between actions.", timeUnit.toSeconds(delay));
        Thread.sleep(timeUnit.toMillis(delay));
    }


    private static void waitUntilNextAction() throws InterruptedException {
        TimeUnit timeUnit = TimeUnit.MILLISECONDS;
        long staticDelay = 10000;
        int maxRandomOffset = 5000;

        long delay = staticDelay + random.nextInt(maxRandomOffset);
        LOGGER.info("wait {} sec between actions.", timeUnit.toSeconds(delay));
        Thread.sleep(timeUnit.toMillis(delay));
    }
}
