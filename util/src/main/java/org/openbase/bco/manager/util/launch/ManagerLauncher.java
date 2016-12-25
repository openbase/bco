
package org.openbase.bco.manager.util.launch;

import org.openbase.bco.manager.agent.core.AgentManagerLauncher;
import org.openbase.bco.manager.app.core.AppManagerLauncher;
import org.openbase.bco.manager.device.core.DeviceManagerLauncher;
import org.openbase.bco.manager.location.core.LocationManagerLauncher;
import org.openbase.bco.manager.scene.core.SceneManagerLauncher;
import org.openbase.bco.manager.user.core.UserManagerLauncher;
import org.openbase.bco.registry.lib.launch.AbstractLauncher;
import org.openbase.jul.pattern.Manager;

/**
 * #%L
 * #L%
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class ManagerLauncher {
     /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        AbstractLauncher.main(args, Manager.class,
                AgentManagerLauncher.class,
                AppManagerLauncher.class,
                DeviceManagerLauncher.class,
                LocationManagerLauncher.class,
                SceneManagerLauncher.class,
                UserManagerLauncher.class
        );
    }
}