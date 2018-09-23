package org.openbase.bco.app.openhab;

/*-
 * #%L
 * BCO Openhab App
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

import org.openbase.bco.app.openhab.manager.OpenHABDeviceManagerLauncher;
import org.openbase.bco.app.openhab.registry.OpenHABConfigSynchronizerLauncher;
import org.openbase.bco.app.openhab.sitemap.OpenHABSitemapSynchronizerLauncher;
import org.openbase.bco.app.openhab.sitemap.SitemapSynchronizer;
import org.openbase.bco.registry.lib.BCO;
import org.openbase.jps.core.JPService;
import org.openbase.jps.preset.JPVerbose;
import org.openbase.jul.pattern.launch.AbstractLauncher;

public class OpenHABApp {

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        BCO.printLogo();
        JPService.registerProperty(JPVerbose.class, true);
        AbstractLauncher.main(args, OpenHABApp.class,
                OpenHABConfigSynchronizerLauncher.class,
                OpenHABDeviceManagerLauncher.class,
                OpenHABSitemapSynchronizerLauncher.class
        );
    }
}
