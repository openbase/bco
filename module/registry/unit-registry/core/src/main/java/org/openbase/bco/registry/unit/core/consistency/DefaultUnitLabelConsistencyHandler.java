package org.openbase.bco.registry.unit.core.consistency;

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

import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

import java.util.Locale;

/**
 * Default label consistency handler for units. This consistency handler makes sure that a unit has at least one label
 * that is not empty and that all labels are unique for a location.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class DefaultUnitLabelConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    @Override
    public void processData(final String id,
                            final IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry,
                            final ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap,
                            final ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry)
            throws CouldNotPerformException, EntryModification {
        final UnitConfig unitConfig = entry.getMessage();

        // check if has at least one label not empty
        if (!unitConfig.hasLabel() || LabelProcessor.isEmpty(unitConfig.getLabel())) {
            // add default label
            UnitConfig.Builder unitConfigBuilder = unitConfig.toBuilder().clearLabel();
            LabelProcessor.addLabel(unitConfigBuilder.getLabelBuilder(), Locale.ENGLISH, generateDefaultLabel(unitConfig));
            throw new EntryModification(entry.setMessage(unitConfigBuilder, this), this);
        }

        if (!unitConfig.hasPlacementConfig() || !unitConfig.getPlacementConfig().hasLocationId() || unitConfig.getPlacementConfig().getLocationId().isEmpty()) {
            throw new NotAvailableException("unit.placement.locationId");
        }
    }

    /**
     * This method generates a default label for a unit config.
     * This method returns the first unit alias.
     * It can be overwritten by sub classes to generate different default label.
     *
     * @param unitConfig the unit config for which a label is generated
     *
     * @return a default label
     *
     * @throws CouldNotPerformException if no alias is available
     */
    protected String generateDefaultLabel(final UnitConfig unitConfig) throws CouldNotPerformException {
        if (unitConfig.getAliasCount() < 1) {
            throw new InvalidStateException("Alias not provided by Unit[" + unitConfig.getId() + "]!");
        }
        return UnitConfigProcessor.getDefaultAlias(unitConfig, "?");
    }
}
