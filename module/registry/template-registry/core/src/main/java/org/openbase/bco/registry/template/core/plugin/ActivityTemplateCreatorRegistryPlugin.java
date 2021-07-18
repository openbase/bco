package org.openbase.bco.registry.template.core.plugin;

/*-
 * #%L
 * BCO Registry Template Core
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.jul.storage.registry.plugin.ProtobufRegistryPluginAdapter;
import org.openbase.type.domotic.activity.ActivityTemplateType.ActivityTemplate;
import org.openbase.type.domotic.activity.ActivityTemplateType.ActivityTemplate.ActivityType;
import org.openbase.type.domotic.activity.ActivityTemplateType.ActivityTemplate.Builder;
import org.openbase.type.domotic.registry.TemplateRegistryDataType.TemplateRegistryData;

public class ActivityTemplateCreatorRegistryPlugin extends ProtobufRegistryPluginAdapter<String, ActivityTemplate, Builder> {

    private final ProtoBufFileSynchronizedRegistry<String, ActivityTemplate, ActivityTemplate.Builder, TemplateRegistryData.Builder> registry;

    public ActivityTemplateCreatorRegistryPlugin(ProtoBufFileSynchronizedRegistry<String, ActivityTemplate, Builder, TemplateRegistryData.Builder> unitTemplateRegistry) {
        this.registry = unitTemplateRegistry;
    }

    @Override
    public void init(ProtoBufRegistry<String, ActivityTemplate, Builder> config) throws InitializationException, InterruptedException {
        try {
            ActivityTemplate template;

            // create missing unit template
            if (registry.size() <= ActivityType.values().length - 1) {
                for (ActivityType activityType : ActivityType.values()) {
                    if (activityType == ActivityType.UNKNOWN) {
                        continue;
                    }
                    template = ActivityTemplate.newBuilder().setActivityType(activityType).build();
                    if (!containsActivityTemplateByType(activityType)) {
                        registry.register(template);
                    }
                }
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException("Could not init " + getClass().getSimpleName() + "!", ex);
        }
    }

    private boolean containsActivityTemplateByType(ActivityType activityType) throws CouldNotPerformException {
        for (ActivityTemplate activityTemplate : registry.getMessages()) {
            if (activityTemplate.getActivityType() == activityType) {
                return true;
            }
        }
        return false;
    }
}
