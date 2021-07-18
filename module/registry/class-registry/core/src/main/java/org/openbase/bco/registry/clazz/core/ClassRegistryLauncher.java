package org.openbase.bco.registry.clazz.core;

/*
 * #%L
 * BCO Registry Class Core
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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

import org.openbase.bco.registry.clazz.lib.ClassRegistry;
import org.openbase.bco.registry.clazz.lib.jp.JPAgentClassDatabaseDirectory;
import org.openbase.bco.registry.clazz.lib.jp.JPAppClassDatabaseDirectory;
import org.openbase.bco.registry.clazz.lib.jp.JPClassRegistryScope;
import org.openbase.bco.registry.clazz.lib.jp.JPDeviceClassDatabaseDirectory;
import org.openbase.bco.authentication.lib.BCO;
import org.openbase.bco.registry.lib.launch.AbstractRegistryLauncher;
import org.openbase.jps.core.JPService;
import org.openbase.jps.preset.JPDebugMode;
import org.openbase.jps.preset.JPForce;
import org.openbase.jps.preset.JPReadOnly;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.extension.rsb.com.jp.JPRSBHost;
import org.openbase.jul.extension.rsb.com.jp.JPRSBPort;
import org.openbase.jul.extension.rsb.com.jp.JPRSBTransport;
import org.openbase.jul.storage.registry.jp.JPDeveloperMode;
import org.openbase.jul.storage.registry.jp.JPGitRegistryPlugin;
import org.openbase.jul.storage.registry.jp.JPGitRegistryPluginRemoteURL;
import org.openbase.jul.storage.registry.jp.JPRecoverDB;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class ClassRegistryLauncher extends AbstractRegistryLauncher<ClassRegistryController> {

    public ClassRegistryLauncher() throws InstantiationException {
        super(ClassRegistry.class, ClassRegistryController.class);
    }

    public static void main(String[] args) throws Throwable {
        BCO.printLogo();
        main(BCO.class, ClassRegistry.class, args, ClassRegistryLauncher.class);
    }

    @Override
    public boolean isCoreLauncher() {
        return true;
    }

    @Override
    public void loadProperties() {
        JPService.registerProperty(JPClassRegistryScope.class);
        JPService.registerProperty(JPAgentClassDatabaseDirectory.class);
        JPService.registerProperty(JPAppClassDatabaseDirectory.class);
        JPService.registerProperty(JPDeviceClassDatabaseDirectory.class);
        JPService.registerProperty(JPReadOnly.class);
        JPService.registerProperty(JPForce.class);
        JPService.registerProperty(JPDeveloperMode.class);
        JPService.registerProperty(JPDebugMode.class);
        JPService.registerProperty(JPRecoverDB.class);
        JPService.registerProperty(JPGitRegistryPlugin.class);
        JPService.registerProperty(JPGitRegistryPluginRemoteURL.class);

        JPService.registerProperty(JPRSBHost.class);
        JPService.registerProperty(JPRSBPort.class);
        JPService.registerProperty(JPRSBTransport.class);
    }
}
