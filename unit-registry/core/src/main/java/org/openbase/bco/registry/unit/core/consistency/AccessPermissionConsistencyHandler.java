package org.openbase.bco.registry.unit.core.consistency;

/*-
 * #%L
 * BCO Registry Unit Core
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
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.type.domotic.authentication.PermissionConfigType.PermissionConfig;
import org.openbase.type.domotic.authentication.PermissionConfigType.PermissionConfig.MapFieldEntry;
import org.openbase.type.domotic.authentication.PermissionType.Permission;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig.Builder;

/**
 * Consistency handler which guarantees that access permissions always include read permissions.
 * It does make sense to be able to control a unit but not to see its status.
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class AccessPermissionConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, Builder> {

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, Builder> entry, ProtoBufMessageMap<String, UnitConfig, Builder> entryMap, ProtoBufRegistry<String, UnitConfig, Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig.Builder unitConfig = entry.getMessage().toBuilder();

        if (unitConfig.hasPermissionConfig()) {
            boolean modification = false;

            PermissionConfig.Builder permissionConfig = unitConfig.getPermissionConfigBuilder();

            if (permissionConfig.hasOtherPermission() && hasAccessWithoutRead(permissionConfig.getOtherPermission())) {
                modification = true;
                permissionConfig.getOtherPermissionBuilder().setRead(true);
            }

            if (permissionConfig.hasOwnerPermission() && hasAccessWithoutRead(permissionConfig.getOwnerPermission())) {
                modification = true;
                permissionConfig.getOwnerPermissionBuilder().setRead(true);
            }

            for (MapFieldEntry.Builder mapFieldEntry : permissionConfig.getGroupPermissionBuilderList()) {
                if (hasAccessWithoutRead(mapFieldEntry.getPermission())) {
                    modification = true;
                    mapFieldEntry.getPermissionBuilder().setRead(true);
                }
            }

            if (modification) {
                throw new EntryModification(entry.setMessage(unitConfig, this), this);
            }
        }
    }

    private boolean hasAccessWithoutRead(final Permission permission) {
        return permission.getAccess() && !permission.getRead();
    }
}
