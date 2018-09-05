package org.openbase.bco.registry.unit.core.consistency.sceneconfig;

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

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.service.ServiceStateDescriptionType.ServiceStateDescription;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitConfigType.UnitConfig.Builder;
import rst.domotic.unit.scene.SceneConfigType.SceneConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Consistency handler validating service state descriptions inside a service config.
 * It removes them if the referenced unit does not exists or the unit type does not match.
 * <p>
 * TODO: further properties could be validated like if the service type is part of the unit type
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class SceneServiceStateConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, Builder> {

    private final List<ProtoBufFileSynchronizedRegistry<String, UnitConfig, Builder, UnitRegistryData.Builder>> unitRegistryList;

    public SceneServiceStateConsistencyHandler(final List<ProtoBufFileSynchronizedRegistry<String, UnitConfig, Builder, UnitRegistryData.Builder>> unitRegistryList) {
        this.unitRegistryList = unitRegistryList;
    }

    @Override
    public void processData(final String id, final IdentifiableMessage<String, UnitConfig, Builder> entry,
                            final ProtoBufMessageMap<String, UnitConfig, Builder> entryMap,
                            final ProtoBufRegistry<String, UnitConfig, Builder> registry) throws CouldNotPerformException, EntryModification {
        final UnitConfig.Builder sceneUnitConfig = entry.getMessage().toBuilder();
        final SceneConfig.Builder sceneConfig = sceneUnitConfig.getSceneConfigBuilder();

        boolean modification = false;
        // verify that all required service state descriptions are valid
        List<ServiceStateDescription> serviceStateDescriptionList = new ArrayList<>(sceneConfig.getRequiredServiceStateDescriptionList());
        sceneConfig.clearRequiredServiceStateDescription();
        for (final ServiceStateDescription serviceStateDescription : serviceStateDescriptionList) {
            UnitConfig unitConfig = null;
            for (final ProtoBufFileSynchronizedRegistry<String, UnitConfig, Builder, UnitRegistryData.Builder> unitRegistry : unitRegistryList) {
                if (unitRegistry.contains(serviceStateDescription.getUnitId())) {
                    unitConfig = unitRegistry.getMessage(serviceStateDescription.getUnitId());
                    break;
                }
            }

            if (unitConfig == null) {
                modification = true;
                continue;
            }

            if (unitConfig.getUnitType() != serviceStateDescription.getUnitType()) {
                modification = true;
                continue;
            }

            sceneConfig.addRequiredServiceStateDescription(serviceStateDescription);
        }

        // verify that all optional service state descriptions are valid
        serviceStateDescriptionList = new ArrayList<>(sceneConfig.getOptionalServiceStateDescriptionList());
        sceneConfig.clearOptionalServiceStateDescription();
        for (final ServiceStateDescription serviceStateDescription : serviceStateDescriptionList) {
            UnitConfig unitConfig = null;
            for (final ProtoBufFileSynchronizedRegistry<String, UnitConfig, Builder, UnitRegistryData.Builder> unitRegistry : unitRegistryList) {
                if (unitRegistry.contains(serviceStateDescription.getUnitId())) {
                    unitConfig = unitRegistry.getMessage(serviceStateDescription.getUnitId());
                    break;
                }
            }

            if (unitConfig == null) {
                modification = true;
                continue;
            }

            if (unitConfig.getUnitType() != serviceStateDescription.getUnitType()) {
                modification = true;
                continue;
            }

            sceneConfig.addOptionalServiceStateDescription(serviceStateDescription);
        }

        if (modification) {
            throw new EntryModification(entry.setMessage(sceneUnitConfig), this);
        }
    }
}
