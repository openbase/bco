package org.openbase.bco.registry.activity.lib;

/*
 * #%L
 * BCO Registry Activity Library
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import org.openbase.bco.registry.lib.provider.activity.ActivityConfigCollectionProvider;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.iface.Shutdownable;
import org.openbase.jul.iface.annotations.RPCMethod;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.jul.storage.registry.RegistryService;
import rst.domotic.activity.ActivityConfigType.ActivityConfig;
import rst.domotic.registry.ActivityRegistryDataType.ActivityRegistryData;

import java.util.concurrent.Future;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface ActivityRegistry extends ActivityConfigCollectionProvider, DataProvider<ActivityRegistryData>, Shutdownable, RegistryService {

    // ===================================== ActivityConfig Methods ==============================================================================================

    /**
     * Method registers the given activity config.
     *
     * @param activityConfig the activity config to be registered.
     * @return the registered activity config.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    @RPCMethod
    Future<ActivityConfig> registerActivityConfig(ActivityConfig activityConfig) throws CouldNotPerformException;

    /**
     * Method updates the given activity config.
     *
     * @param activityConfig the updated activity config.
     * @return the updated activity config.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    @RPCMethod
    Future<ActivityConfig> updateActivityConfig(ActivityConfig activityConfig) throws CouldNotPerformException;

    /**
     * Method removes the given activity config.
     *
     * @param activityConfig the activity config to be removed.
     * @return the removed activity config.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    @RPCMethod
    Future<ActivityConfig> removeActivityConfig(ActivityConfig activityConfig) throws CouldNotPerformException;

    /**
     * Method returns true if the underlying registry is marked as read only.
     *
     * @return if the activity config registry is read only
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    public Boolean isActivityConfigRegistryReadOnly() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as consistent.
     *
     * @return if the user activity config registry is consistent
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    public Boolean isActivityConfigRegistryConsistent() throws CouldNotPerformException;

}
