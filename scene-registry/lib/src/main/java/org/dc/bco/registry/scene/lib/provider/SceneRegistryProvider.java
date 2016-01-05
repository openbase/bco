package org.dc.bco.registry.scene.lib.provider;

import org.dc.bco.registry.scene.lib.SceneRegistry;
import org.dc.jul.exception.NotAvailableException;

/**
 * Interface provides a globally managed scene registry instance.
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public interface SceneRegistryProvider {

    /**
     * Returns the globally managed scene registry instance.
     * @return
     * @throws NotAvailableException 
     */
    public SceneRegistry getSceneRegistry() throws NotAvailableException;
}
