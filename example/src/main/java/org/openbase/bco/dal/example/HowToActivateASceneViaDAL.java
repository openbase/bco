package org.openbase.bco.dal.example;

/*-
 * #%L
 * BCO DAL Example
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
import org.openbase.bco.dal.remote.action.RemoteAction;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.remote.layer.unit.scene.SceneRemote;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.remote.login.BCOLogin;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.List;
import java.util.concurrent.*;

/**
 *
 * This howto shows how to list all available scenes and how to activate a scene via the bco-dal-remote api.
 *
 * Note: This howto requires a running bco platform provided by your network.
 * Note: If your setup does not provide a scene unit called \"WatchingTV"\ you
 * can use the command-line tool \"bco-query Scene\" to get a list of available scenes in your setup.
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class HowToActivateASceneViaDAL {

    private static final Logger LOGGER = LoggerFactory.getLogger(HowToActivateASceneViaDAL.class);

    public static void howto() throws InterruptedException {

        final SceneRemote testScene;
        try {
            LOGGER.info("wait for registry connection...");
            Registries.waitForData();

            LOGGER.info("authenticate current session...");
            BCOLogin.getSession().loginUserViaUsername("admin", "admin", false);

            final List<UnitConfig> sceneUnitConfigList = Registries.getUnitRegistry().getUnitConfigsByUnitType(UnitType.SCENE);

            if(sceneUnitConfigList.isEmpty()) {
                LOGGER.warn("No scenes available in your current setup! Please create a new one in order to activate it!");
                return;
            }

            LOGGER.info("print all scenes");
            for (UnitConfig sceneUnitConfig : sceneUnitConfigList) {
                LOGGER.info("found Scene[{}] with Alias[{}]", LabelProcessor.getBestMatch(sceneUnitConfig.getLabel()), sceneUnitConfig.getAlias(0));
            }

            LOGGER.info("request a specific scene via its alias \"Scene-9\"");
            testScene = Units.getUnitByAlias("Scene-9", true, Units.SCENE);

            LOGGER.info("activate the scene");

            final Future<ActionDescription> actionFuture = testScene.setActivationState(State.ACTIVE);

            LOGGER.info("wait until action is executing...");
            new RemoteAction(actionFuture).waitForExecution();

        } catch (CouldNotPerformException | CancellationException ex) {
            ExceptionPrinter.printHistory(ex, LOGGER);
        }
    }

    public static void main(String[] args) throws InterruptedException {

        // Setup CLParser
        JPService.setApplicationName("howto");
        JPService.parseAndExitOnError(args);

        // Start HowTo
        LOGGER.info("start " + JPService.getApplicationName());
        howto();
        LOGGER.info("finish " + JPService.getApplicationName());
        System.exit(0);
    }
}
