package org.openbase.bco.app.util.launch;

/*
 * #%L
 * BCO App Utility
 * %%
 * Copyright (C) 2018 - 2019 openbase.org
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

import org.openbase.bco.authentication.core.AuthenticatorLauncher;
import org.openbase.bco.dal.control.layer.unit.agent.AgentManagerLauncher;
import org.openbase.bco.dal.control.layer.unit.app.AppManagerLauncher;
import org.openbase.bco.dal.control.layer.unit.device.DeviceManagerLauncher;
import org.openbase.bco.dal.control.layer.unit.location.LocationManagerLauncher;
import org.openbase.bco.dal.control.layer.unit.scene.SceneManagerLauncher;
import org.openbase.bco.dal.control.layer.unit.user.UserManagerLauncher;
import org.openbase.bco.registry.activity.core.ActivityRegistryLauncher;
import org.openbase.bco.registry.clazz.core.ClassRegistryLauncher;
import org.openbase.bco.authentication.lib.BCO;
import org.openbase.bco.registry.message.core.MessageRegistryLauncher;
import org.openbase.bco.registry.template.core.TemplateRegistryLauncher;
import org.openbase.bco.registry.unit.core.UnitRegistryLauncher;
import org.openbase.jul.pattern.launch.AbstractLauncher;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class BCOTestLauncher {

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        BCO.printLogo();
        AbstractLauncher.main(args, BCO.class,
                AuthenticatorLauncher.class,
                /**
                 * Registry *
                 */
                TemplateRegistryLauncher.class,
                TemplateRegistryLauncher.class,
                ClassRegistryLauncher.class,
                ActivityRegistryLauncher.class,
                UnitRegistryLauncher.class,
                MessageRegistryLauncher.class,
                /**
                 * Manager *
                 */
                AgentManagerLauncher.class,
                AppManagerLauncher.class,
                DeviceManagerLauncher.class,
                LocationManagerLauncher.class,
                SceneManagerLauncher.class,
                UserManagerLauncher.class
        );
    }
}
