package org.openbase.bco.authentification.core;

import org.openbase.bco.registry.lib.BCO;
import org.openbase.bco.registry.lib.launch.AbstractLauncher;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.bco.authenticator.lib.iface.AuthenticatorInterface;
import org.openbase.bco.authenticator.lib.jp.JPAuthentificationScope;
import org.openbase.jps.core.JPService;

/*-
 * #%L
 * BCO Authentification Core
 * %%
 * Copyright (C) 2017 openbase.org
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
 * @author Tamino Huxohl <thuxohl@techfak.uni-bielefel.de>
 */
public class AuthenticatorLauncher extends AbstractLauncher<AuthenticatorController> {

    public AuthenticatorLauncher() throws InstantiationException {
        super(AuthenticatorInterface.class, AuthenticatorController.class);
    }

    @Override
    protected void loadProperties() {
        JPService.registerProperty(JPAuthentificationScope.class);
    }

    /**
     * @param args the command line arguments
     * @throws java.lang.InterruptedException
     * @throws org.openbase.jul.exception.CouldNotPerformException
     */
    public static void main(final String[] args) throws InterruptedException, CouldNotPerformException {
        BCO.printLogo();
        AbstractLauncher.main(args, AuthenticatorInterface.class, AuthenticatorLauncher.class);
    }
}
