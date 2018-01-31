package org.openbase.bco.registry.lib.provider;

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import rst.domotic.unit.UnitConfigType.UnitConfig;

public interface RootLocationConfigProvider {
    /**
     * Method returns the root location of the registered location hierarchy
     * tree.
     *
     * @return the root location
     * @throws CouldNotPerformException is thrown if the request fails.
     * @throws NotAvailableException is thrown if no rood connection exists.
     */
    public UnitConfig getRootLocationConfig() throws CouldNotPerformException, NotAvailableException;

}
