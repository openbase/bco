package org.openbase.bco.registry.remote;

import org.openbase.bco.registry.agent.remote.AgentRegistryRemote;
import org.openbase.bco.registry.agent.remote.CachedAgentRegistryRemote;
import org.openbase.bco.registry.app.remote.AppRegistryRemote;
import org.openbase.bco.registry.app.remote.CachedAppRegistryRemote;
import org.openbase.bco.registry.device.remote.CachedDeviceRegistryRemote;
import org.openbase.bco.registry.device.remote.DeviceRegistryRemote;
import org.openbase.bco.registry.location.remote.CachedLocationRegistryRemote;
import org.openbase.bco.registry.location.remote.LocationRegistryRemote;
import org.openbase.bco.registry.scene.remote.CachedSceneRegistryRemote;
import org.openbase.bco.registry.scene.remote.SceneRegistryRemote;
import org.openbase.bco.registry.unit.remote.CachedUnitRegistryRemote;
import org.openbase.bco.registry.unit.remote.UnitRegistryRemote;
import org.openbase.bco.registry.user.remote.CachedUserRegistryRemote;
import org.openbase.bco.registry.user.remote.UserRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;

/**
 * #%L
 * #L%
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class Registries {

    /**
     * Returns an initialized and activated remote registry.
     *
     * @return the remote registry instance.
     * @throws NotAvailableException
     * @throws InterruptedException is thrown if thread is externally interrupted.
     */
    public static UnitRegistryRemote getUnitRegistry() throws NotAvailableException, InterruptedException {
        return CachedUnitRegistryRemote.getRegistry();
    }

    /**
     * Returns an initialized and activated remote registry.
     *
     * @return the remote registry instance.
     * @throws NotAvailableException
     * @throws InterruptedException is thrown if thread is externally interrupted.
     */
    public static AgentRegistryRemote getAgentRegistry() throws NotAvailableException, InterruptedException {
        return CachedAgentRegistryRemote.getRegistry();
    }

    /**
     * Returns an initialized and activated remote registry.
     *
     * @return the remote registry instance.
     * @throws NotAvailableException
     * @throws InterruptedException is thrown if thread is externally interrupted.
     */
    public static AppRegistryRemote getAppRegistry() throws NotAvailableException, InterruptedException {
        return CachedAppRegistryRemote.getRegistry();
    }

    /**
     * Returns an initialized and activated remote registry.
     *
     * @return the remote registry instance.
     * @throws NotAvailableException
     * @throws InterruptedException is thrown if thread is externally interrupted.
     */
    public static DeviceRegistryRemote getDeviceRegistry() throws NotAvailableException, InterruptedException {
        return CachedDeviceRegistryRemote.getRegistry();
    }

    /**
     * Returns an initialized and activated remote registry.
     *
     * @return the remote registry instance.
     * @throws NotAvailableException
     * @throws InterruptedException is thrown if thread is externally interrupted.
     */
    public static LocationRegistryRemote getLocationRegistry() throws NotAvailableException, InterruptedException {
        return CachedLocationRegistryRemote.getRegistry();
    }

    /**
     * Returns an initialized and activated remote registry.
     *
     * @return the remote registry instance.
     * @throws NotAvailableException
     * @throws InterruptedException is thrown if thread is externally interrupted.
     */
    public static SceneRegistryRemote getSceneRegistry() throws NotAvailableException, InterruptedException {
        return CachedSceneRegistryRemote.getRegistry();
    }

    /**
     * Returns an initialized and activated remote registry.
     *
     * @return the remote registry instance.
     * @throws NotAvailableException
     * @throws InterruptedException is thrown if thread is externally interrupted.
     */
    public static UserRegistryRemote getUserRegistry() throws NotAvailableException, InterruptedException {
        return CachedUserRegistryRemote.getRegistry();
    }

    /**
     * Method shutdown all registry instances.
     *
     * Please use method with care!
     * Make sure no other instances are using the cached remote instances before shutdown.
     *
     * There is generally no need for the manual registry shutdown because the registries take care of the shutdown.
     */
    public static void shutdown() {
        CachedUnitRegistryRemote.shutdown();
        CachedAgentRegistryRemote.shutdown();
        CachedAppRegistryRemote.shutdown();
        CachedDeviceRegistryRemote.shutdown();
        CachedLocationRegistryRemote.shutdown();
        CachedSceneRegistryRemote.shutdown();
        CachedUserRegistryRemote.shutdown();
    }

    /**
     * Method only returns if all available registries are synchronized.
     *
     * @throws CouldNotPerformException
     * @throws InterruptedException is thrown if thread is externally interrupted.
     */
    public static void waitForData() throws CouldNotPerformException, InterruptedException {
        CachedUnitRegistryRemote.waitForData();
        CachedAgentRegistryRemote.waitForData();
        CachedAppRegistryRemote.waitForData();
        CachedDeviceRegistryRemote.waitForData();
        CachedLocationRegistryRemote.waitForData();
        CachedSceneRegistryRemote.waitForData();
        CachedUserRegistryRemote.waitForData();
    }
}
