package org.openbase.bco.registry.unit.core.consistency.userconfig;

/*
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

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.type.domotic.authentication.PermissionConfigType.PermissionConfig;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

/**
 * Consistency handler which makes sure that if users do not have a permission config a default one is generated.
 * Additionally it guarantees that users always own themselves and that they have all permissions for themselves.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class UserPermissionConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    /**
     * {@inheritDoc}
     *
     * @param id       {@inheritDoc}
     * @param entry    {@inheritDoc}
     * @param entryMap {@inheritDoc}
     * @param registry {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     * @throws EntryModification        {@inheritDoc}
     */
    @Override
    public void processData(final String id, final IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, final ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, final ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        final UnitConfig.Builder unitConfig = entry.getMessage().toBuilder();

        // if no permission config is available generate a default one
        if (!unitConfig.hasPermissionConfig()) {
            unitConfig.setPermissionConfig(generateDefaultUserPermissionConfig(unitConfig.getId()));
            throw new EntryModification(entry.setMessage(unitConfig, this), this);
        }

        boolean modification = false;
        // set owner id to the id of the user
        if (!unitConfig.getPermissionConfig().getOwnerId().equals(unitConfig.getId())) {
            unitConfig.getPermissionConfigBuilder().setOwnerId(unitConfig.getId());
            modification = true;
        }

        // guarantee read permissions
        if (!unitConfig.getPermissionConfig().getOwnerPermission().getRead()) {
            unitConfig.getPermissionConfigBuilder().getOwnerPermissionBuilder().setRead(true);
            modification = true;
        }

        // guarantee write permissions
        if (!unitConfig.getPermissionConfig().getOwnerPermission().getWrite()) {
            unitConfig.getPermissionConfigBuilder().getOwnerPermissionBuilder().setWrite(true);
            modification = true;
        }

        // guarantee access permissions
        if (!unitConfig.getPermissionConfig().getOwnerPermission().getAccess()) {
            unitConfig.getPermissionConfigBuilder().getOwnerPermissionBuilder().setAccess(true);
            modification = true;
        }

        if (modification) {
            throw new EntryModification(entry.setMessage(unitConfig, this), this);
        }
    }

    /**
     * Generate a default permission config for a user. This permission config is owned by the user and gives
     * the user all permissions while denying other everything.
     *
     * @param userId the id of the user for whom the permission config is generated.
     *
     * @return a default permission config tailored to a user.
     */
    private PermissionConfig generateDefaultUserPermissionConfig(final String userId) {
        final PermissionConfig.Builder permissionConfigBuilder = PermissionConfig.newBuilder();
        permissionConfigBuilder.setOwnerId(userId);
        permissionConfigBuilder.getOwnerPermissionBuilder().setWrite(true).setAccess(true).setRead(true);
        permissionConfigBuilder.getOtherPermissionBuilder().setWrite(false).setAccess(false).setRead(false);
        return permissionConfigBuilder.build();
    }
}
