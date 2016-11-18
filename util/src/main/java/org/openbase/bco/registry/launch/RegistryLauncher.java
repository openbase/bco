package org.openbase.bco.registry.launch;

/*
 * #%L
 * BCO Registry Utility
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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
import org.openbase.bco.registry.agent.core.AgentRegistryLauncher;
import org.openbase.bco.registry.app.core.AppRegistryLauncher;
import org.openbase.bco.registry.device.core.DeviceRegistryLauncher;
import org.openbase.bco.registry.user.core.UserRegistryLauncher;
import org.openbase.bco.registry.lib.launch.AbstractLauncher;
import org.openbase.bco.registry.location.core.LocationRegistryLauncher;
import org.openbase.bco.registry.scene.core.SceneRegistryLauncher;
import org.openbase.bco.registry.unit.core.UnitRegistryLauncher;
import org.openbase.jul.storage.registry.Registry;

public class RegistryLauncher {

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        AbstractLauncher.main(args, Registry.class,
                DeviceRegistryLauncher.class,
                AppRegistryLauncher.class,
                AgentRegistryLauncher.class,
                UnitRegistryLauncher.class,
                LocationRegistryLauncher.class,
                UserRegistryLauncher.class,
                SceneRegistryLauncher.class
        );
    }
}
