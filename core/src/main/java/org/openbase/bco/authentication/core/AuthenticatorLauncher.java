package org.openbase.bco.authentication.core;

import org.openbase.bco.authentication.lib.AuthenticationService;
import org.openbase.bco.authentication.lib.jp.JPAuthenticationScope;
import org.openbase.bco.authentication.lib.jp.JPCredentialsDirectory;
import org.openbase.bco.authentication.lib.jp.JPResetCredentials;
import org.openbase.bco.authentication.lib.jp.JPSessionTimeout;
import org.openbase.jps.core.JPService;
import org.openbase.jps.preset.JPPrefix;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.pattern.launch.AbstractLauncher;

/*-
 * #%L
 * BCO Authentication Core
 * %%
 * Copyright (C) 2017 - 2018 openbase.org
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
/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class AuthenticatorLauncher extends AbstractLauncher<AuthenticatorController> {

    public AuthenticatorLauncher() throws InstantiationException {
        super(AuthenticationService.class, AuthenticatorController.class);
    }

    @Override
    protected void loadProperties() {
        JPService.registerProperty(JPPrefix.class);
        JPService.registerProperty(JPAuthenticationScope.class);
        JPService.registerProperty(JPResetCredentials.class);
        JPService.registerProperty(JPCredentialsDirectory.class);
        JPService.registerProperty(JPSessionTimeout.class);
    }

    /**
     * @param args the command line arguments
     * @throws java.lang.InterruptedException
     * @throws org.openbase.jul.exception.CouldNotPerformException
     */
    public static void main(final String[] args) throws InterruptedException, CouldNotPerformException {
//        BCO.printLogo();
        AbstractLauncher.main(args, AuthenticationService.class, AuthenticatorLauncher.class);
    }
}
