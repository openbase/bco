package org.openbase.bco.app.util.launch;

/*
 * #%L
 * BCO App Utility
 * %%
 * Copyright (C) 2018 openbase.org
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

import org.openbase.app.test.agent.AgentManagerLauncher;
import org.openbase.app.test.app.AppManagerLauncher;
import org.openbase.bco.authentication.core.AuthenticatorLauncher;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.dal.control.layer.unit.device.DeviceManagerLauncher;
import org.openbase.bco.dal.control.layer.unit.location.LocationManagerLauncher;
import org.openbase.bco.dal.control.layer.unit.scene.SceneManagerLauncher;
import org.openbase.bco.dal.control.layer.unit.user.UserManagerLauncher;
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.jp.JPProviderControlMode;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.remote.layer.unit.location.LocationRemote;
import org.openbase.bco.registry.activity.core.ActivityRegistryLauncher;
import org.openbase.bco.registry.clazz.core.ClassRegistryLauncher;
import org.openbase.bco.registry.lib.BCO;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.remote.login.BCOLogin;
import org.openbase.bco.registry.template.core.TemplateRegistryLauncher;
import org.openbase.bco.registry.unit.core.UnitRegistryLauncher;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.pattern.launch.AbstractLauncher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.state.PresenceStateType.PresenceState;
import rst.domotic.state.PresenceStateType.PresenceState.State;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.vision.HSBColorType.HSBColor;

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

        BCOLogin.autoLogin(true);

        try {

            LOGGER.info("waiting for registry synchronization...");
            Registries.waitUntilReady();

            LOGGER.info("init simulation setup");

            // init
            LocationRemote location = Units.getUnit(Registries.getUnitRegistry(true).getUnitConfigByAlias("location-3"), true, Units.LOCATION);
            final ActionDescription absentState = ActionDescriptionProcessor.generateActionDescriptionBuilder(
                    PresenceState.newBuilder().setValue(State.ABSENT).build(),
                    ServiceType.PRESENCE_STATE_SERVICE,
                    location).build();
            final ActionDescription presentState = ActionDescriptionProcessor.generateActionDescriptionBuilder(
                    PresenceState.newBuilder().setValue(State.PRESENT).build(),
                    ServiceType.PRESENCE_STATE_SERVICE,
                    location).build();
            //
            location.setColor(HSBColor.newBuilder().setBrightness(100).setSaturation(0).build());
            boolean present = false;


            LOGGER.info("generate actions...");

            while (!Thread.interrupted()) {

                LOGGER.info("handle present == " + present);

                try {

                    // check condition
                    if (present) {

                        // random order
                        if (random.nextBoolean()) {
                            // human leaving the room
                            location.applyAction(absentState).get();
                            // and after a while
                            waitBetweenActions();
                            // they is switching the light off.
                            location.setPowerState(PowerState.State.OFF, UnitType.LIGHT).get();
                        } else {

                            // human is switching the light off.
                            location.setPowerState(PowerState.State.OFF, UnitType.LIGHT).get();
                            // and after a while
                            waitBetweenActions();
                            // they is leaving the room
                            location.applyAction(absentState).get();
                        }
                    } else {
                        // random order
                        if (random.nextBoolean()) {
                            // human entering the room
                            location.applyAction(presentState).get();
                            // and after a while
                            waitBetweenActions();
                            // they is switching the light on.
                            location.setPowerState(PowerState.State.ON, UnitType.LIGHT).get();
                        } else {
                            // human is switching the light on
                            location.setPowerState(PowerState.State.ON, UnitType.LIGHT).get();
                            // and after a while
                            waitBetweenActions();
                            // they is entering the room
                            location.applyAction(presentState).get();
                        }
                    }
                } catch (CancellationException | ExecutionException ex) {
                    ExceptionPrinter.printHistory("generator run skipped!", ex, LOGGER);
                }
                // toggle condition
                present = !present;
                waitUntilNextAction();
            }

        } catch (InterruptedException e) {
            // halt
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Training data generation failed!", ex, LOGGER);
        }
    }

    private static void waitBetweenActions() throws InterruptedException {
        TimeUnit timeUnit = TimeUnit.MILLISECONDS;
        long staticDelay = 1000;
        int maxRandomOffset = 5000;

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
