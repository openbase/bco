package org.openbase.bco.registry.login;

import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.authentication.lib.jp.JPAuthentication;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.core.plugin.UserCreationPlugin;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class SystemLogin {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystemLogin.class);

    public static void loginBCOUser() throws CouldNotPerformException, InterruptedException {

        // check if authenication is enabled.
        try {
            if (!JPService.getProperty(JPAuthentication.class).getValue()) {
                return;
            }
        } catch (JPNotAvailableException ex) {
            ExceptionPrinter.printHistory("Could not load " + JPAuthentication.class.getSimpleName(), ex, LOGGER, LogLevel.WARN);
        }

        Registries.getUnitRegistry().waitForData();

        for (final UnitConfig userUnitConfig : Registries.getUnitRegistry().getUnitConfigs(UnitTemplateType.UnitTemplate.UnitType.USER)) {
            if (userUnitConfig.getUserConfig().getUserName().equals(UserCreationPlugin.BCO_USERNAME)) {
                SessionManager.getInstance().login(userUnitConfig.getId());
                return;
            }
        }

        // system login not possible!
    }

}
