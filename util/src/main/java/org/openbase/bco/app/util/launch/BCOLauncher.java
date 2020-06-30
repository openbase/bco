package org.openbase.bco.app.util.launch;

/*
 * #%L
 * BCO App Utility
 * %%
 * Copyright (C) 2018 - 2020 openbase.org
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

import org.openbase.bco.app.util.launch.jp.JPDeviceManager;
import org.openbase.bco.app.util.launch.jp.JPDeviceManager.BuildinDeviceManager;
import org.openbase.bco.authentication.core.AuthenticatorLauncher;
import org.openbase.bco.authentication.lib.BCO;
import org.openbase.bco.dal.control.layer.unit.agent.AgentManagerLauncher;
import org.openbase.bco.dal.control.layer.unit.app.AppManagerLauncher;
import org.openbase.bco.dal.control.layer.unit.location.LocationManagerLauncher;
import org.openbase.bco.dal.control.layer.unit.scene.SceneManagerLauncher;
import org.openbase.bco.dal.control.layer.unit.user.UserManagerLauncher;
import org.openbase.bco.device.openhab.OpenHABDeviceManagerLauncher;
import org.openbase.bco.device.openhab.registry.OpenHABConfigSynchronizerLauncher;
import org.openbase.bco.device.openhab.sitemap.OpenHABSitemapSynchronizerLauncher;
import org.openbase.bco.registry.activity.core.ActivityRegistryLauncher;
import org.openbase.bco.registry.clazz.core.ClassRegistryLauncher;
import org.openbase.bco.registry.message.core.MessageRegistryLauncher;
import org.openbase.bco.registry.template.core.TemplateRegistryLauncher;
import org.openbase.bco.registry.unit.core.UnitRegistryLauncher;
import org.openbase.jps.core.JPService;
import org.openbase.jul.pattern.launch.AbstractLauncher;

import java.util.ArrayList;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class BCOLauncher {

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        BCO.printLogo();

        // create dynamic launcher container
        ArrayList<Class<? extends AbstractLauncher>> launcher = new ArrayList<>();

        /**
         * Configure Authenticator Launcher
         */
        launcher.add(AuthenticatorLauncher.class);

        /**
         * Configure Registry Launcher
         */
        launcher.add(TemplateRegistryLauncher.class);
        launcher.add(TemplateRegistryLauncher.class);
        launcher.add(ClassRegistryLauncher.class);
        launcher.add(ActivityRegistryLauncher.class);
        launcher.add(UnitRegistryLauncher.class);
        launcher.add(MessageRegistryLauncher.class);

        /**
         * Configure Manager Launcher
         */
        launcher.add(AgentManagerLauncher.class);
        launcher.add(AppManagerLauncher.class);
        launcher.add(LocationManagerLauncher.class);
        launcher.add(SceneManagerLauncher.class);
        launcher.add(UserManagerLauncher.class);

        /**
         * Configure dynamic Device Manager Launcher
         */
        // register device manager property
        JPService.registerProperty(JPDeviceManager.class);

        // pre evaluate device manager selection in order to preload modules.
        switch (JPService.getPreEvaluatedValue(JPDeviceManager.class, args, BuildinDeviceManager.NON)) {
            case OPENHAB:
                launcher.add(OpenHABDeviceManagerLauncher.class);
                launcher.add(OpenHABConfigSynchronizerLauncher.class);
                launcher.add(OpenHABSitemapSynchronizerLauncher.class);
                break;
            case NON:
            default:
                // skip dynamic launcher registration
        }

        /**
         * Launch BCO
         */
        AbstractLauncher.main(args, BCO.class, launcher.toArray(new Class[launcher.size()]));
    }
}
