package org.openbase.bco.manager.util.launch;

/*
 * #%L
 * BCO Manager Utility
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
import org.openbase.bco.registry.lib.BCO;
import org.openbase.bco.manager.agent.binding.openhab.AgentBindingOpenHABLauncher;
import org.openbase.bco.manager.agent.core.AgentManagerLauncher;
import org.openbase.bco.manager.app.binding.openhab.AppBindingOpenHABLauncher;
import org.openbase.bco.manager.app.core.AppManagerLauncher;
import org.openbase.bco.manager.device.binding.openhab.DeviceBindingOpenHABLauncher;
import org.openbase.bco.manager.device.binding.openhab.util.configgen.OpenHABConfigGeneratorLauncher;
import org.openbase.bco.manager.location.binding.openhab.LocationBindingOpenHABLauncher;
import org.openbase.bco.manager.location.core.LocationManagerLauncher;
import org.openbase.bco.manager.scene.binding.openhab.SceneBindingOpenHABLauncher;
import org.openbase.bco.manager.scene.core.SceneManagerLauncher;
import org.openbase.bco.manager.user.core.UserManagerLauncher;
import org.openbase.bco.registry.agent.core.AgentRegistryLauncher;
import org.openbase.bco.registry.app.core.AppRegistryLauncher;
import org.openbase.bco.registry.device.core.DeviceRegistryLauncher;
import org.openbase.jul.pattern.launch.AbstractLauncher;
import org.openbase.bco.registry.location.core.LocationRegistryLauncher;
import org.openbase.bco.registry.scene.core.SceneRegistryLauncher;
import org.openbase.bco.registry.unit.core.UnitRegistryLauncher;
import org.openbase.bco.registry.user.activity.core.UserActivityRegistryLauncher;
import org.openbase.bco.registry.user.core.UserRegistryLauncher;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class BCOBindingOpenhabLauncher {

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        BCO.printLogo();
        AbstractLauncher.main(args, BCO.class,
                /**
                 * Registry *
                 */
                DeviceRegistryLauncher.class,
                AppRegistryLauncher.class,
                AgentRegistryLauncher.class,
                UnitRegistryLauncher.class,
                LocationRegistryLauncher.class,
                UserRegistryLauncher.class,
                SceneRegistryLauncher.class,
                UserActivityRegistryLauncher.class,
                /**
                 * Manager *
                 */
                AgentManagerLauncher.class,
                AppManagerLauncher.class,
                LocationManagerLauncher.class,
                SceneManagerLauncher.class,
                UserManagerLauncher.class,
                /**
                 * Bindings *
                 */
                AppBindingOpenHABLauncher.class,
                AgentBindingOpenHABLauncher.class,
                LocationBindingOpenHABLauncher.class,
                SceneBindingOpenHABLauncher.class,
                DeviceBindingOpenHABLauncher.class,
                OpenHABConfigGeneratorLauncher.class
        );
    }
}
