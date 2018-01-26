package org.openbase.bco.registry.unit.core.consistency;

/*-
 * #%L
 * BCO Registry Unit Core
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

import org.openbase.bco.authentication.lib.AuthorizationHelper;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import rst.domotic.authentication.PermissionConfigType.PermissionConfig;
import rst.domotic.authentication.PermissionType.Permission;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class UnitPermissionCleanerConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> authorizationGroupConfigRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> locationConfigRegistry;

    public UnitPermissionCleanerConsistencyHandler(
            final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> authorizationGroupConfigRegistry,
            final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> locationConfigRegistry) {
        this.authorizationGroupConfigRegistry = authorizationGroupConfigRegistry;
        this.locationConfigRegistry = locationConfigRegistry;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig.Builder unitConfig = entry.getMessage().toBuilder();

        if (unitConfig.hasPermissionConfig()) {
            PermissionConfig.Builder permissionConfig = unitConfig.getPermissionConfigBuilder();
            boolean modification = false;

            // fill other permission fields that are not set
            if (!permissionIsEmpty(permissionConfig.getOtherPermission())) {
                Permission otherPermission = AuthorizationHelper.getPermission(entry.getMessage(), null, authorizationGroupConfigRegistry.getEntryMap(), locationConfigRegistry.getEntryMap());
                modification = fillEmptyPermissions(permissionConfig.getOtherPermissionBuilder(), otherPermission) || modification;
            }

            // fill owner permissions which are not set
            if (!permissionIsEmpty(permissionConfig.getOwnerPermission())) {
                Permission ownerPermission = AuthorizationHelper.getPermission(entry.getMessage(), permissionConfig.getOwnerId(), authorizationGroupConfigRegistry.getEntryMap(), locationConfigRegistry.getEntryMap());
                modification = fillEmptyPermissions(permissionConfig.getOwnerPermissionBuilder(), ownerPermission) || modification;
            }

            // clean empty permission configs
            if (permissionIsEmpty(permissionConfig.getOtherPermission()) && permissionIsEmpty(permissionConfig.getOwnerPermission()) && permissionConfig.getGroupPermissionList().isEmpty()) {
                unitConfig.clearPermissionConfig();
                modification = true;
            }

            if (modification) {
                throw new EntryModification(entry.setMessage(unitConfig), this);
            }
        }
    }

    private boolean permissionIsEmpty(Permission permission) {
        return !permission.hasAccess() && !permission.hasRead() && !permission.hasWrite();
    }

    private boolean fillEmptyPermissions(Permission.Builder permissionBuilder, Permission permission) {
        boolean modification = false;
        if (!permissionBuilder.hasAccess()) {
            permissionBuilder.setAccess(permission.getAccess());
            modification = true;
        }
        if (!permissionBuilder.hasRead()) {
            permissionBuilder.setRead(permission.getRead());
            modification = true;
        }
        if (!permissionBuilder.hasWrite()) {
            permissionBuilder.setWrite(permission.getWrite());
            modification = true;
        }
        return modification;
    }

}
