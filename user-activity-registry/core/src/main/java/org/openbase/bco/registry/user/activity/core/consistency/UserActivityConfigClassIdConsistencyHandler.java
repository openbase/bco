package org.openbase.bco.registry.user.activity.core.consistency;

/*-
 * #%L
 * BCO Registry User Activity Core
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

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import rst.domotic.activity.UserActivityClassType.UserActivityClass;
import rst.domotic.activity.UserActivityConfigType.UserActivityConfig;
import rst.domotic.activity.UserActivityConfigType.UserActivityConfig.Builder;
import rst.domotic.registry.UserActivityRegistryDataType.UserActivityRegistryData;

/**
 * Consistency handler validating that a UserActivityCOnfig has a class id
 * and the class to that id exists.
 *
 * @author <a href="mailto:pLeminoq@openbase.org">Tamino Huxohl</a>
 */
public class UserActivityConfigClassIdConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UserActivityConfig, UserActivityConfig.Builder> {

    private final ProtoBufFileSynchronizedRegistry<String, UserActivityClass, UserActivityClass.Builder, UserActivityRegistryData.Builder> userActivityClassRegistry;

    public UserActivityConfigClassIdConsistencyHandler(ProtoBufFileSynchronizedRegistry<String, UserActivityClass, UserActivityClass.Builder, UserActivityRegistryData.Builder> userActivityClassRegistry) {
        this.userActivityClassRegistry = userActivityClassRegistry;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UserActivityConfig, UserActivityConfig.Builder> entry, ProtoBufMessageMap<String, UserActivityConfig, UserActivityConfig.Builder> entryMap, ProtoBufRegistry<String, UserActivityConfig, Builder> registry) throws CouldNotPerformException, EntryModification {
        if (!entry.getMessage().hasUserActivityClassId()) {
            throw new VerificationFailedException("UserActivityConfig [" + entry.getMessage() + "] has no userActivityClassId!");
        }

        if (!userActivityClassRegistry.contains(entry.getMessage().getUserActivityClassId())) {
            throw new NotAvailableException("UserActivityClass[" + entry.getMessage().getUserActivityClassId() + "]");
        }
    }
}
