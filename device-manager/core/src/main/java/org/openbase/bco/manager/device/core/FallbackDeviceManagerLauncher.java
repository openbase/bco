package org.openbase.bco.manager.device.core;

/*-
 * #%L
 * BCO Manager Device Core
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
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

import org.openbase.bco.dal.lib.jp.JPResourceAllocation;
import org.openbase.bco.manager.device.lib.DeviceManager;
import org.openbase.bco.registry.device.lib.jp.JPDeviceRegistryScope;
import org.openbase.bco.registry.lib.BCO;
import org.openbase.bco.registry.location.lib.jp.JPLocationRegistryScope;
import org.openbase.jps.core.JPService;
import org.openbase.jps.preset.JPPrefix;
import org.openbase.jul.pattern.launch.AbstractLauncher;

/**
 *
 * @author <a href="mailto:pLeminoq@openbase.org">Tamino Huxohl</a>
 */
public class FallbackDeviceManagerLauncher extends AbstractLauncher<FallbackDeviceManagerController> {

    public FallbackDeviceManagerLauncher() throws org.openbase.jul.exception.InstantiationException {
        super(DeviceManager.class, FallbackDeviceManagerController.class);
    }

    @Override
    public void loadProperties() {
        JPService.registerProperty(JPPrefix.class);
        JPService.registerProperty(JPLocationRegistryScope.class);
        JPService.registerProperty(JPDeviceRegistryScope.class);
        JPService.registerProperty(JPResourceAllocation.class);
    }

    public static void main(String args[]) throws Throwable {
        BCO.printLogo();
        main(args, FallbackDeviceManagerLauncher.class, FallbackDeviceManagerLauncher.class);
    }
}
