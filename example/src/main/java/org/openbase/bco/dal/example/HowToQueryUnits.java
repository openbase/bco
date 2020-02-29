package org.openbase.bco.dal.example;

/*-
 * #%L
 * BCO DAL Example
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
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

import org.openbase.bco.dal.remote.layer.unit.scene.SceneRemote;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.remote.login.BCOLogin;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CancellationException;


/**
 *
 * This howto demonstrates how the configuration of units of a specific type can be queried to perform any further processing.
 *
 * Note: This howto requires a running bco platform provided by your network.
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class HowToQueryUnits {

    private static final Logger LOGGER = LoggerFactory.getLogger(HowToQueryUnits.class);

    public static void howto() throws InterruptedException {

        try {
            LOGGER.info("wait for registry connection...");
            Registries.waitForData();

            LOGGER.info("authenticate current session...");
            BCOLogin.getSession().loginUserViaUsername("admin", "admin", false);

            // First of all we need an id of the location to query.
            final String locationId = Registries.getUnitRegistry().getRootLocationConfig().getId();

            // Then, we have to declare the unit type we want to lookup.
            // For example LIGHT which also includes COLORABLE_LIGHTs and DIMMABLE_LIGHTs.
            final UnitType unitType = UnitType.LIGHT;

            // now we are ready lookup all light at the specific location. For sure you can use the location alias as well to specify the target location
            // or lookup units via its ServiceType instead of its UnitType. For this, please checkout the provided methods of the UnitRegistry.
            LOGGER.info("query lights");
            final List<UnitConfig> lightUnitConfigList = Registries.getUnitRegistry().getUnitConfigsByLocationIdAndUnitType(locationId, unitType);

            // check if we got some results
            if(lightUnitConfigList.isEmpty()) {
                LOGGER.warn("No lights available in your current setup! Please create one in order to query it!");
                return;
            }

            // print all queried lights.
            LOGGER.info("print all lights");
            for (UnitConfig lightUnitConfig : lightUnitConfigList) {
                LOGGER.info("found Light[{}] with Alias[{}]", LabelProcessor.getBestMatch(lightUnitConfig.getLabel()), lightUnitConfig.getAlias(0));
            }

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
