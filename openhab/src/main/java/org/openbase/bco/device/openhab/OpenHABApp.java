package org.openbase.bco.device.openhab;

/*-
 * #%L
 * BCO Openhab Device Manager
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
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

import org.openbase.bco.device.openhab.manager.OpenHABDeviceManagerLauncher;
import org.openbase.bco.device.openhab.registry.OpenHABConfigSynchronizerLauncher;
import org.openbase.bco.device.openhab.sitemap.OpenHABSitemapSynchronizerLauncher;
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
