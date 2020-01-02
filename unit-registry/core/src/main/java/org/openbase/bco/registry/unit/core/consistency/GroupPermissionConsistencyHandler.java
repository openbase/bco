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

import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.type.domotic.authentication.PermissionConfigType.PermissionConfig;
import org.openbase.type.domotic.authentication.PermissionType.Permission;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class GroupPermissionConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    private static final Permission ALL_PERMISSION = Permission.newBuilder().setAccess(true).setRead(true).setWrite(true).build();

    private final Map<String, String> aliasIdMap;

    public GroupPermissionConsistencyHandler(final Map<String, String> aliasIdMap) {
        this.aliasIdMap = aliasIdMap;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        final UnitConfig.Builder unitConfig = entry.getMessage().toBuilder();

        // Set permissions only for the root location and units without a location.
        if ((!unitConfig.hasLocationConfig() || !unitConfig.getLocationConfig().getRoot())
                && unitConfig.hasPlacementConfig() && unitConfig.getPlacementConfig().hasLocationId()) {
            return;
        }

        // do nothing if the groups cannot be resolved
        if (aliasIdMap.containsKey(UnitRegistry.ADMIN_GROUP_ALIAS) || !aliasIdMap.containsKey(UnitRegistry.BCO_GROUP_ALIAS)) {
            return;
        }

        final PermissionConfig.Builder permissionConfig = unitConfig.getPermissionConfigBuilder();
        final String adminGroupId = aliasIdMap.get(UnitRegistry.ADMIN_GROUP_ALIAS);
        final String bcoGroupId = aliasIdMap.get(UnitRegistry.BCO_GROUP_ALIAS);

        boolean modification = false;
        boolean containsAdminGroup = false;
        boolean containsBCOGroup = false;
        final List<PermissionConfig.MapFieldEntry> groupPermissionList = permissionConfig.getGroupPermissionList();
        permissionConfig.clearGroupPermission();
        for (final PermissionConfig.MapFieldEntry mapFieldEntry : groupPermissionList) {
            if (mapFieldEntry.getGroupId().equals(adminGroupId)) {
                containsAdminGroup = true;
                if (!mapFieldEntry.getPermission().equals(ALL_PERMISSION)) {
                    permissionConfig.addGroupPermission(mapFieldEntry.toBuilder().setPermission(ALL_PERMISSION));
                    modification = true;
                }
            } else if (mapFieldEntry.getGroupId().equals(bcoGroupId)) {
                containsBCOGroup = true;
                if (!mapFieldEntry.getPermission().equals(ALL_PERMISSION)) {
                    permissionConfig.addGroupPermission(mapFieldEntry.toBuilder().setPermission(ALL_PERMISSION));
                    modification = true;
                }
            } else {
                permissionConfig.addGroupPermission(mapFieldEntry);
            }
        }

        if (!containsAdminGroup) {
            permissionConfig.addGroupPermission(PermissionConfig.MapFieldEntry.newBuilder().setGroupId(adminGroupId).setPermission(ALL_PERMISSION));
            modification = true;
        }
        if (!containsBCOGroup) {
            permissionConfig.addGroupPermission(PermissionConfig.MapFieldEntry.newBuilder().setGroupId(bcoGroupId).setPermission(ALL_PERMISSION));
            modification = true;
        }

        if (modification) {
            throw new EntryModification(entry.setMessage(unitConfig, this), this);
        }
    }
}
