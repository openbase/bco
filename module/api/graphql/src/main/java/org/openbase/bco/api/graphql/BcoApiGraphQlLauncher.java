package org.openbase.bco.api.graphql;

/*-
 * #%L
 * BCO GraphQL API
 * %%
 * Copyright (C) 2020 openbase.org
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
import org.openbase.bco.authentication.lib.jp.JPCredentialsDirectory;
import org.openbase.jps.core.JPService;
import org.openbase.jps.preset.JPDebugMode;
import org.openbase.jul.communication.jp.JPComHost;
import org.openbase.jul.communication.jp.JPComPort;
import org.openbase.jul.pattern.launch.AbstractLauncher;

public class BcoApiGraphQlLauncher extends AbstractLauncher<BcoApiGraphQlSpringLaunchable>  {

    public BcoApiGraphQlLauncher() throws org.openbase.jul.exception.InstantiationException {
        super(BcoApiGraphQlLauncher.class, BcoApiGraphQlSpringLaunchable.class);
    }

    @Override
    protected void loadProperties() {
        JPService.registerProperty(JPDebugMode.class);
        JPService.registerProperty(JPCredentialsDirectory.class);
        JPService.registerProperty(JPComHost.class);
        JPService.registerProperty(JPComPort.class);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        BCO.printLogo();
        AbstractLauncher.main(BCO.class, BcoApiGraphQlLauncher.class, args, BcoApiGraphQlLauncher.class);
    }
}
