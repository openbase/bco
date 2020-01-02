package org.openbase.bco.registry.lib.provider.activity;

/*-
 * #%L
 * BCO Registry Lib
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
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

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.annotation.RPCMethod;
import org.openbase.type.domotic.activity.ActivityConfigType.ActivityConfig;
import org.openbase.type.domotic.activity.ActivityTemplateType.ActivityTemplate.ActivityType;

import java.util.List;

public interface ActivityConfigCollectionProvider {

    /**
     * Method returns true if the activity config with the given id is
     * registered, otherwise false. The activity config id field is used for the
     * comparison.
     *
     * Note: Method returns true in case the registry is not available. Maybe you need to check this in advance.
     *
     * @param activityConfig the activity config which is tested
     * @return if the activity config with the given id is registered, otherwise false
     */
    @RPCMethod
    Boolean containsActivityConfig(ActivityConfig activityConfig);

    /**
     * Method returns true if the activity config with the given id is
     * registered, otherwise false.
     *
     * Note: Method returns true in case the registry is not available. Maybe you need to check this in advance.
     *
     * @param activityConfigId the id of the activity config
     * @return if the activity config with the given id is registered, otherwise false
     */
    @RPCMethod
    Boolean containsActivityConfigById(String activityConfigId);

    /**
     * Method returns the activity config which is registered with the given
     * id.
     *
     * @param activityConfigId the id of the activity config
     * @return the requested activity config.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    @RPCMethod
    ActivityConfig getActivityConfigById(final String activityConfigId) throws CouldNotPerformException;

    /**
     * Method returns all registered activity configs.
     *
     * @return the activity configs stored in this registry.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    List<ActivityConfig> getActivityConfigs() throws CouldNotPerformException;

    /**
     * Method returns the all activity configs with the given type.
     *
     * @param activityType the activity type
     * @return a list of activity configs with the given activity type
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    List<ActivityConfig> getActivityConfigsByType(final ActivityType activityType) throws CouldNotPerformException;
}
