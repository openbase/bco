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
import org.openbase.jps.preset.JPDebugMode;
import org.openbase.jps.preset.JPVerbose;
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

/*
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class BCOTrainDataGeneratorLauncher {

    public static final Logger LOGGER = LoggerFactory.getLogger(BCOTrainDataGeneratorLauncher.class);
    private static final Random random = new Random(System.currentTimeMillis());
    public static final long TIMEOUT = 1000;

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
        JPService.registerProperty(JPDebugMode.class, false);
        JPService.registerProperty(JPVerbose.class, false);
        JPService.parse(args);

        BCOLogin.getSession().autoLogin(true);

        try {
            LOGGER.info("please make sure bco is started with the --provider-control flag, otherwise no provider services can be synthesised.");
            LOGGER.info("waiting for registry synchronization...");
            Registries.waitUntilReady();

            // init
            final int trainingSetCounter = 10;
            LOGGER.info("init simulation of {} runs with {} conditions.", trainingSetCounter, trainConditions.size());

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
                                if (conditionOrder()) {
                                    Actions.waitForExecution(location.applyAction(absentState), TIMEOUT, TimeUnit.MILLISECONDS);
                                    Actions.waitForExecution(location.applyAction(darkState), TIMEOUT, TimeUnit.MILLISECONDS);
                                    waitBetweenActions();
                                    Actions.waitForExecution(light.setPowerState(PowerState.State.OFF), TIMEOUT, TimeUnit.MILLISECONDS);
                                } else {
                                    Actions.waitForExecution(light.setPowerState(PowerState.State.OFF), TIMEOUT, TimeUnit.MILLISECONDS);
                                    waitBetweenActions();
                                    Actions.waitForExecution(location.applyAction(absentState), TIMEOUT, TimeUnit.MILLISECONDS);
                                    Actions.waitForExecution(location.applyAction(darkState), TIMEOUT, TimeUnit.MILLISECONDS);
                                }
                                break;
                            case PRESENCE_DARK_ON:
                                if (conditionOrder()) {
                                    Actions.waitForExecution(location.applyAction(presentState), TIMEOUT, TimeUnit.MILLISECONDS);
                                    Actions.waitForExecution(location.applyAction(darkState), TIMEOUT, TimeUnit.MILLISECONDS);
                                    waitBetweenActions();
                                    Actions.waitForExecution(light.setPowerState(PowerState.State.ON), TIMEOUT, TimeUnit.MILLISECONDS);
                                } else {
                                    Actions.waitForExecution(light.setPowerState(PowerState.State.ON), TIMEOUT, TimeUnit.MILLISECONDS);
                                    waitBetweenActions();
                                    Actions.waitForExecution(location.applyAction(presentState), TIMEOUT, TimeUnit.MILLISECONDS);
                                    Actions.waitForExecution(location.applyAction(darkState), TIMEOUT, TimeUnit.MILLISECONDS);
                                }
                                break;
                            case ABSENCE_SUNNY_OFF:
                                if (conditionOrder()) {
                                    Actions.waitForExecution(location.applyAction(absentState), TIMEOUT, TimeUnit.MILLISECONDS);
                                    Actions.waitForExecution(location.applyAction(sunnyState), TIMEOUT, TimeUnit.MILLISECONDS);
                                    waitBetweenActions();
                                    Actions.waitForExecution(light.setPowerState(PowerState.State.OFF), TIMEOUT, TimeUnit.MILLISECONDS);
                                } else {
                                    Actions.waitForExecution(light.setPowerState(PowerState.State.OFF), TIMEOUT, TimeUnit.MILLISECONDS);
                                    waitBetweenActions();
                                    Actions.waitForExecution(location.applyAction(absentState), TIMEOUT, TimeUnit.MILLISECONDS);
                                    Actions.waitForExecution(location.applyAction(sunnyState), TIMEOUT, TimeUnit.MILLISECONDS);
                                }
                                break;
                            case PRESENCE_SUNNY_OFF:
                                if (conditionOrder()) {
                                    Actions.waitForExecution(location.applyAction(presentState), TIMEOUT, TimeUnit.MILLISECONDS);
                                    Actions.waitForExecution(location.applyAction(sunnyState), TIMEOUT, TimeUnit.MILLISECONDS);
                                    waitBetweenActions();
                                    Actions.waitForExecution(light.setPowerState(PowerState.State.OFF), TIMEOUT, TimeUnit.MILLISECONDS);
                                } else {
                                    Actions.waitForExecution(light.setPowerState(PowerState.State.OFF), TIMEOUT, TimeUnit.MILLISECONDS);
                                    waitBetweenActions();
                                    Actions.waitForExecution(location.applyAction(presentState), TIMEOUT, TimeUnit.MILLISECONDS);
                                    Actions.waitForExecution(location.applyAction(sunnyState), TIMEOUT, TimeUnit.MILLISECONDS);
                                }
                                break;
                        }
                        waitUntilNextAction();
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

    private static boolean conditionOrder() {
        return random.nextBoolean();
    }

    private static void waitBetweenActions() throws InterruptedException {
        TimeUnit timeUnit = TimeUnit.MILLISECONDS;
        long staticDelay = 1900;
        int maxRandomOffset = 1900;

        long delay = staticDelay + random.nextInt(maxRandomOffset);
        LOGGER.info("wait {} sec between actions.", timeUnit.toSeconds(delay));
        Thread.sleep(timeUnit.toMillis(delay));
    }

    private static void waitUntilNextAction() throws InterruptedException {
        TimeUnit timeUnit = TimeUnit.MILLISECONDS;
        long staticDelay = 6000;
        int maxRandomOffset = 6000;

        long delay = staticDelay + random.nextInt(maxRandomOffset);
        LOGGER.info("wait {} sec until next action.", timeUnit.toSeconds(delay));
        Thread.sleep(timeUnit.toMillis(delay));
    }
}
