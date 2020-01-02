package org.openbase.bco.registry.activity.core.consistency;

/*-
 * #%L
 * BCO Registry Activity Core
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

import org.openbase.bco.registry.template.remote.CachedTemplateRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.type.domotic.activity.ActivityConfigType.ActivityConfig;

/**
 * Consistency handler validating that a ActivityCOnfig has a class id
 * and the class to that id exists.
 *
 * @author <a href="mailto:pLeminoq@openbase.org">Tamino Huxohl</a>
 */
public class ActivityConfigTemplateIdConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, ActivityConfig, ActivityConfig.Builder> {

    public ActivityConfigTemplateIdConsistencyHandler() {
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, ActivityConfig, ActivityConfig.Builder> entry, ProtoBufMessageMap<String, ActivityConfig, ActivityConfig.Builder> entryMap, ProtoBufRegistry<String, ActivityConfig, ActivityConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        if (!entry.getMessage().hasActivityTemplateId()) {
            throw new VerificationFailedException("ActivityConfig [" + entry.getMessage() + "] has no ActivityTemplateId!");
        }

        if (!CachedTemplateRegistryRemote.getRegistry().containsActivityTemplateById(entry.getMessage().getActivityTemplateId())) {
            throw new NotAvailableException("ActivityTemplate[" + entry.getMessage().getActivityTemplateId() + "]");
        }
    }
}
