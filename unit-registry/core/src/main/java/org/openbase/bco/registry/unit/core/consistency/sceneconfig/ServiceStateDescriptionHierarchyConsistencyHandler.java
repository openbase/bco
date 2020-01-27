package org.openbase.bco.registry.unit.core.consistency.sceneconfig;

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

import org.openbase.bco.registry.template.remote.CachedTemplateRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig.Builder;
import org.openbase.type.domotic.unit.scene.SceneConfigType.SceneConfig;

/**
 * This consistency handler makes sure that only one of several colliding service state descriptions is kept in a scene.
 * Required service state descriptions are always preferred over optional ones. Else the service hierarchy as defined
 * via service templates is consulted.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ServiceStateDescriptionHierarchyConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, Builder> {

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, Builder> entry, ProtoBufMessageMap<String, UnitConfig, Builder> entryMap, ProtoBufRegistry<String, UnitConfig, Builder> registry) throws CouldNotPerformException, EntryModification {
        final UnitConfig.Builder unitConfig = entry.getMessage().toBuilder();
        final SceneConfig.Builder sceneConfig = unitConfig.getSceneConfigBuilder();
        boolean modification = false;

        // iterate over all required service state descriptions except the last
        for (int i = 0; i < sceneConfig.getRequiredServiceStateDescriptionCount() - 1; i++) {
            // extract the service type
            final ServiceType serviceTypeI = sceneConfig.getRequiredServiceStateDescription(i).getServiceType();
            final String unitId = sceneConfig.getRequiredServiceStateDescription(i).getUnitId();

            // iterate over all following required service state descriptions
            for (int j = i + 1; j < sceneConfig.getRequiredServiceStateDescriptionCount(); j++) {
                // do nothing if the unit ids differ
                if (!unitId.equals(sceneConfig.getRequiredServiceStateDescription(j).getUnitId())) {
                    continue;
                }

                // extract the second service type
                final ServiceType serviceTypeJ = sceneConfig.getRequiredServiceStateDescription(j).getServiceType();
                if (serviceTypeI == serviceTypeJ || CachedTemplateRegistryRemote.getRegistry().getSuperServiceTypes(serviceTypeI).contains(serviceTypeJ)) {
                    // if the types are equal or the second is a super type of the first, keep the first and continue the inner loop
                    sceneConfig.removeRequiredServiceStateDescription(j);
                    j--;
                    modification = true;
                } else {
                    // else the second has a higher priority, so remove the first and continue the outer loop
                    sceneConfig.removeRequiredServiceStateDescription(i);
                    i--;
                    modification = true;
                    break;
                }
            }
        }

        // remove all optional service state descriptions with the same unit id as required service state descriptions
        for (int i = 0; i < sceneConfig.getRequiredServiceStateDescriptionCount(); i++) {
            for (int j = 0; i < sceneConfig.getOptionalServiceStateDescriptionCount(); j++) {
                if (sceneConfig.getOptionalServiceStateDescription(j).getUnitId().equals(sceneConfig.getRequiredServiceStateDescription(i).getUnitId())) {
                    sceneConfig.removeOptionalServiceStateDescription(j);
                    j--;
                    modification = true;
                }
            }
        }


        // iterate over all optional service state descriptions except the last
        for (int i = 0; i < sceneConfig.getOptionalServiceStateDescriptionCount() - 1; i++) {
            // extract the service type
            final ServiceType serviceTypeI = sceneConfig.getOptionalServiceStateDescription(i).getServiceType();
            final String unitId = sceneConfig.getOptionalServiceStateDescription(i).getUnitId();

            // iterate over all following optional service state descriptions
            for (int j = i + 1; j < sceneConfig.getOptionalServiceStateDescriptionCount(); j++) {
                // do nothing if the unit ids differ
                if (!unitId.equals(sceneConfig.getOptionalServiceStateDescription(j).getUnitId())) {
                    continue;
                }

                // extract the second service type
                final ServiceType serviceTypeJ = sceneConfig.getOptionalServiceStateDescription(j).getServiceType();
                if (serviceTypeI == serviceTypeJ || CachedTemplateRegistryRemote.getRegistry().getSuperServiceTypes(serviceTypeI).contains(serviceTypeJ)) {
                    // if the types are equal or the second is a super type of the first, keep the first and continue the inner loop
                    sceneConfig.removeOptionalServiceStateDescription(j);
                    j--;
                    modification = true;
                } else {
                    // else the second has a higher priority, so remove the first and continue the outer loop
                    sceneConfig.removeOptionalServiceStateDescription(i);
                    i--;
                    modification = true;
                    break;
                }
            }
        }

        if (modification) {
            throw new EntryModification(entry.setMessage(unitConfig, this), this);
        }
    }
}
