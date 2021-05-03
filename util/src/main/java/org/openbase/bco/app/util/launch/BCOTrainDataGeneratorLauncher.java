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

import org.openbase.bco.authentication.lib.BCO;
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.jp.JPProviderControlMode;
import org.openbase.bco.dal.remote.action.Actions;
import org.openbase.bco.dal.remote.layer.unit.ColorableLightRemote;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.remote.layer.unit.location.LocationRemote;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.remote.login.BCOLogin;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.ExceptionProcessor;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.IlluminanceStateType.IlluminanceState;
import org.openbase.type.domotic.state.PowerStateType.PowerState;
import org.openbase.type.domotic.state.PresenceStateType.PresenceState;
import org.openbase.type.vision.HSBColorType.HSBColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class BCOTrainDataGeneratorLauncher {

    public static final Logger LOGGER = LoggerFactory.getLogger(BCOTrainDataGeneratorLauncher.class);
    private static final Random random = new Random(System.currentTimeMillis());
    public static final long TIMEOUT = 2000000000;

    public enum TrainCondition {
        PRESENCE_DARK_ON,
        ABSENCE_DARK_OFF,
        PRESENCE_SUNNY_OFF,
        ABSENCE_SUNNY_OFF
    }

    final static List<TrainCondition> trainConditions = new ArrayList<>(Arrays.asList(TrainCondition.values()));

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) throws JPServiceException {
        BCO.printLogo();
        JPService.registerProperty(JPProviderControlMode.class, true);
        JPService.parse(args);

        BCOLogin.getSession().autoLogin(true);

        try {
            LOGGER.info("please make sure bco is started with the --provider-control flag, otherwise no provider services can be synthesised.");
            LOGGER.info("waiting for registry synchronization...");
            Registries.waitUntilReady();

            LOGGER.info("init simulation setup");


            // init
//            final int trainingSetCounter = 2;
            final int trainingSetCounter = 10;

            LocationRemote location = Units.getUnit(Registries.getUnitRegistry(true).getUnitConfigByAlias("Location-Adhoc"), true, Units.LOCATION);
            ColorableLightRemote light = Units.getUnit(Registries.getUnitRegistry(true).getUnitConfigByAlias("ColorableLight-Adhoc"), true, Units.COLORABLE_LIGHT);

            final ActionDescription absentState = ActionDescriptionProcessor.generateActionDescriptionBuilder(
                    PresenceState.newBuilder().setValue(PresenceState.State.ABSENT).build(),
                    ServiceType.PRESENCE_STATE_SERVICE,
                    location).build();
            final ActionDescription presentState = ActionDescriptionProcessor.generateActionDescriptionBuilder(
                    PresenceState.newBuilder().setValue(PresenceState.State.PRESENT).build(),
                    ServiceType.PRESENCE_STATE_SERVICE,
                    location).build();

            final ActionDescription darkState = ActionDescriptionProcessor.generateActionDescriptionBuilder(
                    IlluminanceState.newBuilder().setValue(IlluminanceState.State.DARK).build(),
                    ServiceType.ILLUMINANCE_STATE_SERVICE,
                    location).build();
            final ActionDescription sunnyState = ActionDescriptionProcessor.generateActionDescriptionBuilder(
                    IlluminanceState.newBuilder().setValue(IlluminanceState.State.SUNNY).build(),
                    ServiceType.ILLUMINANCE_STATE_SERVICE,
                    location).build();

            LOGGER.info("prepare setup...");
            Actions.waitForExecution(location.applyAction(presentState), TIMEOUT, TimeUnit.MILLISECONDS);
            waitBetweenActions();
            Actions.waitForExecution(location.setColor(HSBColor.newBuilder().setBrightness(1).setSaturation(0).build()), TIMEOUT, TimeUnit.MILLISECONDS);
            waitUntilNextAction();

            LOGGER.info("generate " + trainingSetCounter + " training sets.");

            for (int i = 0; i < trainingSetCounter; i++) {

                Collections.shuffle(trainConditions);

                for (TrainCondition trainCondition : trainConditions) {

                    // skip on manual cancel
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }

                    LOGGER.info("=== generate condition {} for training set {} ===", trainCondition.name(), (i + 1));

                    try {
                        // check condition
                        switch (trainCondition) {
                            case ABSENCE_DARK_OFF:
//                                if (random.nextBoolean()) {
                                    Actions.waitForExecution(location.applyAction(absentState), TIMEOUT, TimeUnit.MILLISECONDS);
                                    Actions.waitForExecution(location.applyAction(darkState), TIMEOUT, TimeUnit.MILLISECONDS);
                                    waitBetweenActions();
                                    Actions.waitForExecution(light.setPowerState(PowerState.State.OFF), TIMEOUT, TimeUnit.MILLISECONDS);
//                                } else {
//                                    Actions.waitForExecution(light.setPowerState(PowerState.State.OFF), TIMEOUT, TimeUnit.MILLISECONDS);
//                                    waitBetweenActions();
//                                    Actions.waitForExecution(location.applyAction(absentState), TIMEOUT, TimeUnit.MILLISECONDS);
//                                    Actions.waitForExecution(location.applyAction(darkState), TIMEOUT, TimeUnit.MILLISECONDS);
//                                }
                                break;
                            case PRESENCE_DARK_ON:
//                                if (random.nextBoolean()) {
                                    Actions.waitForExecution(location.applyAction(presentState), TIMEOUT, TimeUnit.MILLISECONDS);
                                    Actions.waitForExecution(location.applyAction(darkState), TIMEOUT, TimeUnit.MILLISECONDS);
                                    waitBetweenActions();
                                    Actions.waitForExecution(light.setPowerState(PowerState.State.ON), TIMEOUT, TimeUnit.MILLISECONDS);
//                                } else {
//                                    Actions.waitForExecution(light.setPowerState(PowerState.State.ON), TIMEOUT, TimeUnit.MILLISECONDS);
//                                    waitBetweenActions();
//                                    Actions.waitForExecution(location.applyAction(presentState), TIMEOUT, TimeUnit.MILLISECONDS);
//                                    Actions.waitForExecution(location.applyAction(darkState), TIMEOUT, TimeUnit.MILLISECONDS);
//                                }
                                break;
                            case ABSENCE_SUNNY_OFF:
//                                if (random.nextBoolean()) {
                                    Actions.waitForExecution(location.applyAction(absentState), TIMEOUT, TimeUnit.MILLISECONDS);
                                    Actions.waitForExecution(location.applyAction(sunnyState), TIMEOUT, TimeUnit.MILLISECONDS);
                                    waitBetweenActions();
                                    Actions.waitForExecution(light.setPowerState(PowerState.State.OFF), TIMEOUT, TimeUnit.MILLISECONDS);
//                                } else {
//                                    Actions.waitForExecution(light.setPowerState(PowerState.State.OFF), TIMEOUT, TimeUnit.MILLISECONDS);
//                                    waitBetweenActions();
//                                    Actions.waitForExecution(location.applyAction(absentState), TIMEOUT, TimeUnit.MILLISECONDS);
//                                    Actions.waitForExecution(location.applyAction(sunnyState), TIMEOUT, TimeUnit.MILLISECONDS);
//                                }
                                break;
                            case PRESENCE_SUNNY_OFF:
//                                if (random.nextBoolean()) {
                                    Actions.waitForExecution(location.applyAction(presentState), TIMEOUT, TimeUnit.MILLISECONDS);
                                    Actions.waitForExecution(location.applyAction(sunnyState), TIMEOUT, TimeUnit.MILLISECONDS);
                                    waitBetweenActions();
                                    Actions.waitForExecution(light.setPowerState(PowerState.State.OFF), TIMEOUT, TimeUnit.MILLISECONDS);
//                                } else {
//                                    Actions.waitForExecution(light.setPowerState(PowerState.State.OFF), TIMEOUT, TimeUnit.MILLISECONDS);
//                                    waitBetweenActions();
//                                    Actions.waitForExecution(location.applyAction(presentState), TIMEOUT, TimeUnit.MILLISECONDS);
//                                    Actions.waitForExecution(location.applyAction(sunnyState), TIMEOUT, TimeUnit.MILLISECONDS);
//                                }
                                break;
                        }
                        waitUntilNextAction();

                        // random order
//                        if (random.nextBoolean()) {
                        // human leaving the room
//                        LOGGER.info("absent send " + System.currentTimeMillis());
//                        Actions.waitForExecution(location.applyAction(darkState), TIMEOUT, TimeUnit.MILLISECONDS);
//                        Actions.waitForExecution(location.applyAction(lightState), TIMEOUT, TimeUnit.MILLISECONDS);
//                        LOGGER.info("absent done " + System.currentTimeMillis());
                        // and after a while

                        // they is switching the light off.
//                            Actions.waitForExecution(location.setPowerState(PowerState.State.OFF, UnitType.LIGHT));
//                        LOGGER.info("off send " + System.currentTimeMillis());
//                        Actions.waitForExecution(light.setPowerState(PowerState.State.OFF), TIMEOUT, TimeUnit.MILLISECONDS);
//                        LOGGER.info("off done " + System.currentTimeMillis());
//                        } else {
//
//                            // human is switching the light off.
////                            Actions.waitForExecution(location.setPowerState(PowerState.State.OFF, UnitType.LIGHT));
//                            Actions.waitForExecution(light.setPowerState(PowerState.State.OFF));
//                            // and after a while
//                            waitBetweenActions();
//                            // they is leaving the room
//                            Actions.waitForExecution(location.applyAction(absentState));
//                        }

//                        } else {
//                            // random order
////                        if (random.nextBoolean()) {
//                            // human entering the room
//                            LOGGER.info("present send "+System.currentTimeMillis());
//                            Actions.waitForExecution(location.applyAction(presentState), TIMEOUT, TimeUnit.MILLISECONDS);
//                            Actions.waitForExecution(location.applyAction(darkState), TIMEOUT, TimeUnit.MILLISECONDS);
//                            LOGGER.info("present done "+System.currentTimeMillis());
//                            // and after a while
//                            waitBetweenActions();
//                            // they is switching the light on.
////                            Actions.waitForExecution(location.setPowerState(PowerState.State.ON, UnitType.LIGHT));
//                            LOGGER.info("on send "+System.currentTimeMillis());
//                            Actions.waitForExecution(light.setPowerState(PowerState.State.ON), TIMEOUT, TimeUnit.MILLISECONDS);
//                            LOGGER.info("on done "+System.currentTimeMillis());
//                        } else {
//                            // human is switching the light on
////                            Actions.waitForExecution(location.setPowerState(PowerState.State.ON, UnitType.LIGHT));
//                            Actions.waitForExecution(light.setPowerState(PowerState.State.ON));
//                            // and after a while
//                            waitBetweenActions();
//                            // they is entering the room
//                            Actions.waitForExecution(location.applyAction(presentState));
//                        }
                    } catch (CancellationException ex) {
                        ExceptionPrinter.printHistory("generator run skipped!", ex, LOGGER);
                    }
                }
            }
            LOGGER.info("generate finished.");
        } catch (InterruptedException e) {
            LOGGER.info("generate canceled by user.");
        } catch (CouldNotPerformException | TimeoutException ex) {
            if (!ExceptionProcessor.isCausedBySystemShutdown(ex)) {
                ExceptionPrinter.printHistory("Training data generation failed!", ex, LOGGER);
            }
            System.exit(1);
        }
        System.exit(0);
    }

    private static void waitBetweenActions() throws InterruptedException {
        TimeUnit timeUnit = TimeUnit.MILLISECONDS;
        long staticDelay = 5000;
        int maxRandomOffset = 2500;

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
