package org.openbase.bco.manager.util.launch;

import org.openbase.bco.manager.agent.binding.openhab.AgentBindingOpenHABLauncher;
import org.openbase.bco.manager.agent.core.AgentManagerLauncher;
import org.openbase.bco.manager.app.core.AppManagerLauncher;
import org.openbase.bco.manager.device.binding.openhab.DeviceBindingOpenHABLauncher;
import org.openbase.bco.manager.device.core.DeviceManagerLauncher;
import org.openbase.bco.manager.location.binding.openhab.LocationBindingOpenHABLauncher;
import org.openbase.bco.manager.location.core.LocationManagerLauncher;
import org.openbase.bco.manager.scene.binding.openhab.SceneBindingOpenHABLauncher;
import org.openbase.bco.manager.scene.core.SceneManagerLauncher;
import org.openbase.bco.manager.user.core.UserManagerLauncher;
import org.openbase.bco.registry.agent.core.AgentRegistryLauncher;
import org.openbase.bco.registry.app.core.AppRegistryLauncher;
import org.openbase.bco.registry.device.core.DeviceRegistryLauncher;
import org.openbase.bco.registry.lib.launch.AbstractLauncher;
import org.openbase.bco.registry.location.core.LocationRegistryLauncher;
import org.openbase.bco.registry.scene.core.SceneRegistryLauncher;
import org.openbase.bco.registry.unit.core.UnitRegistryLauncher;
import org.openbase.bco.registry.user.core.UserRegistryLauncher;

/**
 * #%L
 * #L%
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class BCOLauncher {

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        AbstractLauncher.main(args, BCO.class,
                /** Registry **/
                DeviceRegistryLauncher.class,
                AppRegistryLauncher.class,
                AgentRegistryLauncher.class,
                UnitRegistryLauncher.class,
                LocationRegistryLauncher.class,
                UserRegistryLauncher.class,
                SceneRegistryLauncher.class,
                
                /** Manager **/
                AgentManagerLauncher.class,
                AppManagerLauncher.class,
                DeviceManagerLauncher.class,
                LocationManagerLauncher.class,
                SceneManagerLauncher.class,
                UserManagerLauncher.class, 
                
                /** Bindings **/
                AgentBindingOpenHABLauncher.class,
                LocationBindingOpenHABLauncher.class,
                SceneBindingOpenHABLauncher.class,
                DeviceBindingOpenHABLauncher.class
        );
    }
}
