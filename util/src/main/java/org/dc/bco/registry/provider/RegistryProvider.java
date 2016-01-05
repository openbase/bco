package org.dc.bco.registry.provider;

import org.dc.bco.registry.agent.lib.provider.AgentRegistryProvider;
import org.dc.bco.registry.app.lib.provider.AppRegistryProvider;
import org.dc.bco.registry.device.lib.provider.DeviceRegistryProvider;
import org.dc.bco.registry.location.lib.provider.LocationRegistryProvider;
import org.dc.bco.registry.scene.lib.provider.SceneRegistryProvider;
import org.dc.bco.registry.user.lib.provider.UserRegistryProvider;

/**
 * Interface provides a collection of globally managed registry instances.
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public interface RegistryProvider extends AgentRegistryProvider, AppRegistryProvider, DeviceRegistryProvider, LocationRegistryProvider, SceneRegistryProvider, UserRegistryProvider {

}
