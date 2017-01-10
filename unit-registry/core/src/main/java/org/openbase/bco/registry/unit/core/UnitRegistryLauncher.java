package org.openbase.bco.registry.unit.core;

/*
 * #%L
 * BCO Registry Unit Core
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
 *
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
import org.openbase.bco.registry.lib.com.AbstractRegistryController;
import static org.openbase.bco.registry.lib.launch.AbstractLauncher.main;
import org.openbase.bco.registry.lib.launch.AbstractRegistryLauncher;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.bco.registry.unit.lib.jp.JPAgentConfigDatabaseDirectory;
import org.openbase.bco.registry.unit.lib.jp.JPAppConfigDatabaseDirectory;
import org.openbase.bco.registry.unit.lib.jp.JPAuthorizationGroupConfigDatabaseDirectory;
import org.openbase.bco.registry.unit.lib.jp.JPConnectionConfigDatabaseDirectory;
import org.openbase.bco.registry.unit.lib.jp.JPDalUnitConfigDatabaseDirectory;
import org.openbase.bco.registry.unit.lib.jp.JPDeviceConfigDatabaseDirectory;
import org.openbase.bco.registry.unit.lib.jp.JPLocationConfigDatabaseDirectory;
import org.openbase.bco.registry.unit.lib.jp.JPSceneConfigDatabaseDirectory;
import org.openbase.bco.registry.unit.lib.jp.JPUnitGroupDatabaseDirectory;
import org.openbase.bco.registry.unit.lib.jp.JPUnitRegistryScope;
import org.openbase.bco.registry.unit.lib.jp.JPUnitTemplateDatabaseDirectory;
import org.openbase.jps.core.JPService;
import org.openbase.jps.preset.JPDebugMode;
import org.openbase.jps.preset.JPForce;
import org.openbase.jps.preset.JPReadOnly;
import org.openbase.jul.storage.registry.jp.JPGitRegistryPlugin;
import org.openbase.jul.storage.registry.jp.JPGitRegistryPluginRemoteURL;
import org.openbase.jul.storage.registry.jp.JPInitializeDB;
import org.openbase.jul.storage.registry.jp.JPRecoverDB;

public class UnitRegistryLauncher extends AbstractRegistryLauncher<UnitRegistryController> {

    public UnitRegistryLauncher() throws org.openbase.jul.exception.InstantiationException {
        super(UnitRegistry.class, UnitRegistryController.class);
    }

    @Override
    public void loadProperties() {
        JPService.registerProperty(JPUnitRegistryScope.class);
        JPService.registerProperty(JPReadOnly.class);
        JPService.registerProperty(JPForce.class);
        JPService.registerProperty(JPDebugMode.class);
        JPService.registerProperty(JPRecoverDB.class);
        JPService.registerProperty(JPInitializeDB.class);
        JPService.registerProperty(JPUnitTemplateDatabaseDirectory.class);
        JPService.registerProperty(JPAgentConfigDatabaseDirectory.class);
        JPService.registerProperty(JPAppConfigDatabaseDirectory.class);
        JPService.registerProperty(JPAuthorizationGroupConfigDatabaseDirectory.class);
        JPService.registerProperty(JPConnectionConfigDatabaseDirectory.class);
        JPService.registerProperty(JPDalUnitConfigDatabaseDirectory.class);
        JPService.registerProperty(JPDeviceConfigDatabaseDirectory.class);
        JPService.registerProperty(JPLocationConfigDatabaseDirectory.class);
        JPService.registerProperty(JPSceneConfigDatabaseDirectory.class);
        JPService.registerProperty(JPUnitGroupDatabaseDirectory.class);
        JPService.registerProperty(JPGitRegistryPlugin.class);
        JPService.registerProperty(JPGitRegistryPluginRemoteURL.class);
    }

    public static void main(String args[]) throws Throwable {
        main(args, UnitRegistry.class, UnitRegistryLauncher.class);
    }
}
