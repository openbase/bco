package org.openbase.bco.registry.template.core.consistency.activitytemplate;

/*-
 * #%L
 * BCO Registry Template Core
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
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.type.domotic.activity.ActivityTemplateType.ActivityTemplate;
import org.openbase.type.domotic.activity.ActivityTemplateType.ActivityTemplate.ActivityType;
import org.openbase.type.domotic.activity.ActivityTemplateType.ActivityTemplate.Builder;

import java.util.HashMap;
import java.util.Map;

/**
 * Consistency handler which makes sure that per unit type only one template is registered.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ActivityTemplateUniqueTypeConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, ActivityTemplate, Builder> {

    final Map<ActivityType, ActivityTemplate> activityTypeServiceTemplateMap;

    public ActivityTemplateUniqueTypeConsistencyHandler() {
        this.activityTypeServiceTemplateMap = new HashMap<>();
    }

    @Override
    public void processData(final String id,
                            final IdentifiableMessage<String, ActivityTemplate, Builder> entry,
                            final ProtoBufMessageMap<String, ActivityTemplate, Builder> entryMap, ProtoBufRegistry<String, ActivityTemplate, Builder> registry)
            throws CouldNotPerformException, EntryModification {

        final ActivityTemplate activityTemplate = entry.getMessage();
        if (activityTypeServiceTemplateMap.containsKey(activityTemplate.getActivityType()) && !activityTemplate.getId().equals(activityTypeServiceTemplateMap.get(activityTemplate.getActivityType()).getId())) {
            throw new VerificationFailedException("ActivityTemplate[" + activityTypeServiceTemplateMap.get(activityTemplate.getActivityType()) + "] and activityTemplate[" + activityTemplate + "] both contain the same type");
        }

        activityTypeServiceTemplateMap.put(activityTemplate.getActivityType(), activityTemplate);
    }

    @Override
    public void reset() {
        activityTypeServiceTemplateMap.clear();
    }
}
