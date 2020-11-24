package org.openbase.bco.registry.unit.core.consistency.locationconfig;

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

import org.openbase.bco.registry.unit.core.plugin.UnitUserCreationPlugin;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.extension.type.processing.MetaConfigPool;
import org.openbase.jul.extension.type.processing.MetaConfigVariableProvider;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.type.configuration.MetaConfigType.MetaConfig;
import org.openbase.type.domotic.authentication.PermissionConfigType.PermissionConfig;
import org.openbase.type.domotic.authentication.PermissionConfigType.PermissionConfig.MapFieldEntry;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig.Builder;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.ArrayList;

/**
 * This consistency handler makes sure that permissions from agents and apps are removed on locations where these
 * units are not places. This consistency handler is the counter part to {@link org.openbase.bco.registry.unit.core.plugin.UnitUserCreationPlugin}.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class UnitUserPermissionConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, Builder> {

    private final UnitRegistry unitRegistry;

    public UnitUserPermissionConsistencyHandler(final UnitRegistry unitRegistry) {
        this.unitRegistry = unitRegistry;
    }

    @Override
    public void processData(String id,
                            IdentifiableMessage<String, UnitConfig, Builder> entry,
                            ProtoBufMessageMap<String, UnitConfig, Builder> entryMap,
                            ProtoBufRegistry<String, UnitConfig, Builder> registry) throws CouldNotPerformException, EntryModification {
        final UnitConfig.Builder location = entry.getMessage().toBuilder();

        boolean modification = false;
        final ArrayList<MapFieldEntry> groupPermissions = new ArrayList<>(location.getPermissionConfig().getGroupPermissionList());
        final PermissionConfig.Builder permissionConfigBuilder = location.getPermissionConfigBuilder();

        permissionConfigBuilder.clearGroupPermission();

        for (final MapFieldEntry groupPermission : groupPermissions) {
            // test if the group permission is specified for a user
            try {
                if (unitRegistry.getUnitConfigById(groupPermission.getGroupId()).getUnitType() == UnitType.USER) {
                    // retrieve user
                    final UnitConfig userUnitConfig = unitRegistry.getUnitConfigById(groupPermission.getGroupId());
                    // validate that it is a system user
                    if (userUnitConfig.getUserConfig().getSystemUser()) {
                        try {
                            // retrieve unit belonging to the user
                            final UnitConfig unit = findUnit(userUnitConfig, unitRegistry);
                            // validate that unit is still at the same location
                            if (!unit.getPlacementConfig().getLocationId().equals(entry.getMessage().getId())) {
                                // not at the same location so remove the permissions
                                modification = true;
                                continue;
                            }
                        } catch (NotAvailableException ex) {
                            // keep permissions because they can belong to a system user not associated with a unit
                        }
                    }
                }
            } catch (NotAvailableException ex) {
                // user or group is not available so do not add it again
                continue;
            }

            // re-add permission because its valid
            permissionConfigBuilder.addGroupPermission(groupPermission);
        }

        // if a modification occurred publish it
        if (modification) {
            throw new EntryModification(entry.setMessage(location, this), this);
        }
    }

    private static UnitConfig findUnit(final UnitConfig userUnitConfig, final UnitRegistry unitRegistry) throws NotAvailableException {
        MetaConfigPool metaConfigPool = new MetaConfigPool();
        metaConfigPool.register(new MetaConfigVariableProvider(userUnitConfig.getUserConfig().getUserName() + MetaConfig.class.getSimpleName(), userUnitConfig.getMetaConfig()));
        final String id = metaConfigPool.getValue(UnitUserCreationPlugin.UNIT_ID_KEY);
        return unitRegistry.getUnitConfigById(id);
    }
}
