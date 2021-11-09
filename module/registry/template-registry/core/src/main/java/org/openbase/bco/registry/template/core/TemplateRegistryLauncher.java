package org.openbase.bco.registry.template.core;

/*
 * #%L
 * BCO Registry Template Core
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

import org.openbase.bco.authentication.lib.BCO;
import org.openbase.bco.registry.lib.launch.AbstractRegistryLauncher;
import org.openbase.bco.registry.template.lib.TemplateRegistry;
import org.openbase.bco.registry.template.lib.jp.JPActivityTemplateDatabaseDirectory;
import org.openbase.bco.registry.template.lib.jp.JPServiceTemplateDatabaseDirectory;
import org.openbase.bco.registry.template.lib.jp.JPTemplateRegistryScope;
import org.openbase.bco.registry.template.lib.jp.JPUnitTemplateDatabaseDirectory;
import org.openbase.jps.core.JPService;
import org.openbase.jps.preset.JPDebugMode;
import org.openbase.jps.preset.JPForce;
import org.openbase.jps.preset.JPReadOnly;
import org.openbase.jul.communication.jp.JPComHost;
import org.openbase.jul.communication.jp.JPComPort;
import org.openbase.jul.pattern.launch.AbstractLauncher;
import org.openbase.jul.storage.registry.jp.JPDeveloperMode;
import org.openbase.jul.storage.registry.jp.JPGitRegistryPlugin;
import org.openbase.jul.storage.registry.jp.JPGitRegistryPluginRemoteURL;
import org.openbase.jul.storage.registry.jp.JPRecoverDB;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class TemplateRegistryLauncher extends AbstractRegistryLauncher<TemplateRegistryController> {

    public TemplateRegistryLauncher() throws org.openbase.jul.exception.InstantiationException {
        super(TemplateRegistry.class, TemplateRegistryController.class);
    }

    @Override
    public void loadProperties() {
        JPService.registerProperty(JPTemplateRegistryScope.class);
        JPService.registerProperty(JPActivityTemplateDatabaseDirectory.class);
        JPService.registerProperty(JPServiceTemplateDatabaseDirectory.class);
        JPService.registerProperty(JPUnitTemplateDatabaseDirectory.class);
        JPService.registerProperty(JPReadOnly.class);
        JPService.registerProperty(JPForce.class);
        JPService.registerProperty(JPDebugMode.class);
        JPService.registerProperty(JPDeveloperMode.class);
        JPService.registerProperty(JPRecoverDB.class);
        JPService.registerProperty(JPGitRegistryPlugin.class);
        JPService.registerProperty(JPGitRegistryPluginRemoteURL.class);
        JPService.registerProperty(JPComHost.class);
        JPService.registerProperty(JPComPort.class);
    }

    @Override
    public boolean isCoreLauncher() {
        return true;
    }

    public static void main(String[] args) throws Throwable {
        BCO.printLogo();
        AbstractLauncher.main(BCO.class, TemplateRegistry.class, args, TemplateRegistryLauncher.class);
    }
}
