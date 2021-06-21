package org.openbase.bco.registry.unit.core.consistency.locationconfig;

/*-
 * #%L
 * BCO Registry Unit Core
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

import org.openbase.bco.registry.unit.core.consistency.OtherPermissionConsistencyHandler;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.type.domotic.authentication.PermissionConfigType.PermissionConfig.MapFieldEntry;
import org.openbase.type.domotic.authentication.PermissionType.Permission;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig.Builder;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Consistency handler that verifies the following conditions for the root location permission:
 * - other has always read permissions
 * - the initial user is the owner of the location
 * - the owner, the admin group and the bco group get all permissions
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class RootLocationPermissionConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, Builder> {

    private final Map<String, String> aliasIdMap;

    public RootLocationPermissionConsistencyHandler(final Map<String, String> aliasIdMap) {
        this.aliasIdMap = aliasIdMap;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, Builder> entry, ProtoBufMessageMap<String, UnitConfig, Builder> entryMap, ProtoBufRegistry<String, UnitConfig, Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig.Builder unitConfig = entry.getMessage().toBuilder();
        if (!unitConfig.getLocationConfig().getRoot()) {
            return;
        }

        boolean modification = false;
        if (!unitConfig.hasPermissionConfig()) {
            // generate default other permissions
            unitConfig.getPermissionConfigBuilder().setOtherPermission(OtherPermissionConsistencyHandler.DEFAULT_OTHER_PERMISSION);
            modification = true;
        } else {
            // make sure that read permissions for other are always set
            if (!unitConfig.getPermissionConfig().getOtherPermission().getRead()) {
                unitConfig.getPermissionConfigBuilder().getOtherPermissionBuilder().setRead(true);
                modification = true;
            }
        }

        // the consistency handler can only apply the following changes if the needed authorization groups are already registered
        if (!aliasIdMap.containsKey(UnitRegistry.ADMIN_USER_ALIAS.toLowerCase()) || !aliasIdMap.containsKey(UnitRegistry.BCO_GROUP_ALIAS.toLowerCase())) {
            if (modification) {
                throw new EntryModification(entry.setMessage(unitConfig, this), this);
            } else {
                return;
            }
        }

        // make sure default admin/first user is the owner of the root location
        if (!unitConfig.getPermissionConfig().hasOwnerId()) {
            final String adminId = aliasIdMap.get(UnitRegistry.ADMIN_USER_ALIAS.toLowerCase());
            unitConfig.getPermissionConfigBuilder().setOwnerId(adminId);
            modification = true;
        }

        // make sure owner has all permissions
        final Permission.Builder ownerPermission = unitConfig.getPermissionConfigBuilder().getOwnerPermissionBuilder();
        if (!ownerPermission.getAccess() || !ownerPermission.getRead() || !ownerPermission.getWrite()) {
            ownerPermission.setAccess(true).setRead(true).setWrite(true);
            modification = true;
        }

        final Map<String, Boolean> entryFoundMap = new HashMap<>();
        entryFoundMap.put(aliasIdMap.get(UnitRegistry.ADMIN_GROUP_ALIAS.toLowerCase()), false);
        entryFoundMap.put(aliasIdMap.get(UnitRegistry.BCO_GROUP_ALIAS.toLowerCase()), false);
        // make sure bco user has access and read permission and admin group all permissions
        unitConfig.getPermissionConfigBuilder().clearGroupPermission();
        for (MapFieldEntry mapFieldEntry : entry.getMessage().getPermissionConfig().getGroupPermissionList()) {
            if (entryFoundMap.keySet().contains(mapFieldEntry.getGroupId())) {
                entryFoundMap.put(mapFieldEntry.getGroupId(), true);
                Permission permission = mapFieldEntry.getPermission();
                if (!permission.getAccess() || !permission.getRead() || !permission.getWrite()) {
                    MapFieldEntry.Builder builder = MapFieldEntry.newBuilder().setGroupId(mapFieldEntry.getGroupId());
                    builder.getPermissionBuilder().setAccess(true).setRead(true).setWrite(true);
                    unitConfig.getPermissionConfigBuilder().addGroupPermission(builder);
                    modification = true;
                    continue;
                }
            }
            unitConfig.getPermissionConfigBuilder().addGroupPermission(mapFieldEntry);
        }
        for (Entry<String, Boolean> mapEntry : entryFoundMap.entrySet()) {
            if (!mapEntry.getValue()) {
                MapFieldEntry.Builder builder = MapFieldEntry.newBuilder().setGroupId(mapEntry.getKey());
                builder.getPermissionBuilder().setAccess(true).setRead(true).setWrite(true);
                unitConfig.getPermissionConfigBuilder().addGroupPermission(builder);
                modification = true;
            }
        }

        if (modification) {
            throw new EntryModification(entry.setMessage(unitConfig, this), this);
        }
    }
}
