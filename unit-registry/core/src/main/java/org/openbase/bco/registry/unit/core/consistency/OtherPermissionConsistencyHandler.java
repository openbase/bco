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

import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import rst.domotic.authentication.PermissionConfigType.PermissionConfig;
import rst.domotic.authentication.PermissionType.Permission;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 * Consistency handler which generates other permissions for units without a placement and the root location.
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class OtherPermissionConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    private static final Permission DEFAULT_OTHER_PERMISSION = JPService.testMode() ? Permission.newBuilder().setAccess(true).setRead(true).setWrite(true).build() : Permission.newBuilder().setAccess(false).setRead(true).setWrite(false).build();

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig.Builder unitConfig = entry.getMessage().toBuilder();

        // ignore units with a placement config
        if (unitConfig.hasPlacementConfig() && unitConfig.getPlacementConfig().hasLocationId() && !unitConfig.getPlacementConfig().getLocationId().isEmpty()) {

            // only let root location pass
            boolean isRootLocation = unitConfig.getType() == UnitType.LOCATION && unitConfig.getLocationConfig().getRoot();
            if (!isRootLocation) {
                return;
            }
        }

        PermissionConfig.Builder permissionConfig = unitConfig.getPermissionConfigBuilder();

        if (!permissionConfig.hasOtherPermission()) {
            permissionConfig.setOtherPermission(DEFAULT_OTHER_PERMISSION);
            throw new EntryModification(entry.setMessage(unitConfig), this);
        }
    }
}
