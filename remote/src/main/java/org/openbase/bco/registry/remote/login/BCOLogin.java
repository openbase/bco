package org.openbase.bco.registry.remote.login;

/*-
 * #%L
 * BCO Registry Remote
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
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

import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.authentication.lib.iface.BCOSession;
import org.openbase.bco.authentication.lib.jp.JPAuthentication;
import org.openbase.bco.authentication.lib.jp.JPBCOHomeDirectory;
import org.openbase.bco.registry.lib.jp.JPBCOAutoLoginUser;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.remote.session.BCOSessionImpl;
import org.openbase.bco.registry.remote.session.TokenGenerator;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.type.domotic.authentication.AuthTokenType.AuthToken;
import org.openbase.type.domotic.authentication.LoginCredentialsType.LoginCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class BCOLogin {

    public static final String LOGIN_PROPERTIES = "login.properties";
    public static final String DEFAULT_USER_KEY = "org.openbase.bco.default.user";

    private static final Logger LOGGER = LoggerFactory.getLogger(BCOLogin.class);

    private static Properties loginProperties = new Properties();
    private static BCOSession session;

    static {
        loadLoginProperties();
        session = new BCOSessionImpl(loginProperties);
    }

    public static BCOSession getSession() {
        return session;
    }

    private static void loadLoginProperties() {
        try {
            final File propertiesFile = new File(JPService.getProperty(JPBCOHomeDirectory.class).getValue(), LOGIN_PROPERTIES);
            if (propertiesFile.exists()) {
                loginProperties.load(new FileInputStream(propertiesFile));
                LOGGER.debug("Load login properties from " + propertiesFile.getAbsolutePath());
            }
        } catch (Exception ex) {
            ExceptionPrinter.printHistory("No login properties found!", ex, LOGGER);
        }
    }
}
