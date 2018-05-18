package org.openbase.bco.registry.activity.core.consistency;

/*-
 * #%L
 * BCO Registry Activity Core
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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

import org.openbase.bco.registry.lib.com.SynchronizedRemoteRegistry;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import rst.domotic.activity.ActivityConfigType.ActivityConfig;
import rst.domotic.activity.ActivityTemplateType.ActivityTemplate;

/**
 * Consistency handler validating that a UserActivityCOnfig has a class id
 * and the class to that id exists.
 *
 * @author <a href="mailto:pLeminoq@openbase.org">Tamino Huxohl</a>
 */
public class UserActivityConfigClassIdConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, ActivityConfig, ActivityConfig.Builder> {

    private final SynchronizedRemoteRegistry<String, ActivityTemplate, ActivityTemplate.Builder> activityTemplateRegistry;

    public UserActivityConfigClassIdConsistencyHandler(SynchronizedRemoteRegistry<String, ActivityTemplate, ActivityTemplate.Builder> activityTemplateRegistry) {
        this.activityTemplateRegistry = activityTemplateRegistry;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, ActivityConfig, ActivityConfig.Builder> entry, ProtoBufMessageMap<String, ActivityConfig, ActivityConfig.Builder> entryMap, ProtoBufRegistry<String, ActivityConfig, ActivityConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        if (!entry.getMessage().hasActivityTemplateId()) {
            throw new VerificationFailedException("ActivityConfig [" + entry.getMessage() + "] has no ActivityTemplateId!");
        }

        if (!activityTemplateRegistry.contains(entry.getMessage().getActivityTemplateId())) {
            throw new NotAvailableException("ActivityTemplate[" + entry.getMessage().getActivityTemplateId() + "]");
        }
    }
}
