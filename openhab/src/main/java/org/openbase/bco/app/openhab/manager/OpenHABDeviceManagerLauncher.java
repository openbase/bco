package org.openbase.bco.app.openhab.manager;

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

import org.openbase.bco.app.openhab.jp.JPOpenHABURI;
import org.openbase.bco.registry.lib.BCO;
import org.openbase.jps.core.JPService;
import org.openbase.jul.pattern.launch.AbstractLauncher;

public class OpenHABDeviceManagerLauncher extends AbstractLauncher<OpenHABDeviceManager> {

    public OpenHABDeviceManagerLauncher() throws org.openbase.jul.exception.InstantiationException {
        super(OpenHABDeviceManagerLauncher.class, OpenHABDeviceManager.class);
    }

    @Override
    protected void loadProperties() {
        JPService.registerProperty(JPOpenHABURI.class);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        BCO.printLogo();
        AbstractLauncher.main(args, OpenHABDeviceManagerLauncher.class, OpenHABDeviceManagerLauncher.class);
    }
}
