package org.openbase.bco.app.cloud.connector;

/*-
 * #%L
 * BCO Cloud Connector
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

import org.openbase.bco.app.cloud.connector.jp.JPCloudServerURI;
import org.openbase.bco.authentication.lib.jp.JPAuthentication;
import org.openbase.bco.registry.lib.BCO;
import org.openbase.bco.registry.remote.login.BCOLogin;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 * @deprecated this is just a workaround which should not be used anymore if the cloud connector is started by the app manager
 */
@Deprecated
public class CloudConnectorLauncher {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudConnectorLauncher.class);

    /**
     * Main method which starts the cloud connector.
     *
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        BCO.printLogo();

        //TODO: where should the cloud server uri property be registered? or should this be part of the config?
        JPService.registerProperty(JPCloudServerURI.class);
        JPService.registerProperty(JPAuthentication.class);

        JPService.parseAndExitOnError(args);

        try {
            Registries.waitForData();

            BCOLogin.loginBCOUser();

            CloudConnectorAppImpl cloudConnectorApp = new CloudConnectorAppImpl();
            cloudConnectorApp.init(CloudConnectorAppRemote.getCloudConnectorUnitConfig());
            cloudConnectorApp.enable();
        } catch (InterruptedException | CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not launch cloud connector", ex, LOGGER);
        }

        LOGGER.info(CloudConnectorLauncher.class.getSimpleName() + " successfully started!");
    }
}
