package org.openbase.bco.registry.lib.provider.template;

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
import org.openbase.type.domotic.activity.ActivityTemplateType.ActivityTemplate;
import org.openbase.type.domotic.activity.ActivityTemplateType.ActivityTemplate.ActivityType;

import java.util.List;

public interface ActivityTemplateCollectionProvider {

    /**
     * Method returns true if the activity template with the given id is
     * registered, otherwise false. The activity template id field is used for the
     * comparison.
     *
     * Note: Method returns true in case the registry is not available. Maybe you need to check this in advance.
     *
     * @param activityTemplate the activity template which is tested
     * @return if the activity template with the given id is registered, otherwise false
     */
    @RPCMethod
    Boolean containsActivityTemplate(ActivityTemplate activityTemplate);

    /**
     * Method returns true if the activity template with the given id is
     * registered, otherwise false.
     *
     * Note: Method returns true in case the registry is not available. Maybe you need to check this in advance.
     *
     * @param activityTemplateId the id of the activity template
     * @return if the activity template with the given id is registered, otherwise false
     */
    @RPCMethod
    Boolean containsActivityTemplateById(String activityTemplateId);

    /**
     * Method returns the activity template which is registered with the given
     * id.
     *
     * @param activityTemplateId the id of the activity template
     * @return the requested activity template.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    @RPCMethod
    ActivityTemplate getActivityTemplateById(final String activityTemplateId) throws CouldNotPerformException;

    /**
     * Method returns all registered activity template.
     *
     * @return the activity templates stored in this registry.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    List<ActivityTemplate> getActivityTemplates() throws CouldNotPerformException;

    /**
     * Method returns the activity template with the given type.
     *
     * @param activityType the activity type
     * @return the requested activity template.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    @RPCMethod
    ActivityTemplate getActivityTemplateByType(final ActivityType activityType) throws CouldNotPerformException;
}
