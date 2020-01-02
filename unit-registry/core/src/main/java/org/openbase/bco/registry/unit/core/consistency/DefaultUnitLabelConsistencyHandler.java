package org.openbase.bco.registry.unit.core.consistency;

/*
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

import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.type.language.LabelType.Label;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Default label consistency handler for units. This consistency handler makes sure that a unit has at least one label
 * that is not empty and that all labels are unique for a location.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class DefaultUnitLabelConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    private final Map<String, UnitConfig> unitMap;

    public DefaultUnitLabelConsistencyHandler() {
        this.unitMap = new HashMap<>();
    }

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

        // check if every label of this unit are unique for its location
        for (Label.MapFieldEntry mapEntry : unitConfig.getLabel().getEntryList()) {
            for (final String label : mapEntry.getValueList()) {
                final String key = generateKey(label, mapEntry.getKey(), unitConfig);

                if (unitMap.containsKey(key)) {
                    final String typeName = StringProcessor.transformUpperCaseToPascalCase(unitConfig.getUnitType().name());
                    throw new InvalidStateException(
                            typeName + "[" + UnitConfigProcessor.getDefaultAlias(unitConfig, "?") + "] and " +
                                    typeName + "[" + unitMap.get(key).getAlias(0) +
                                    "] are registered with the same label according to the generated key[" +
                                    key + "].");
                }

                unitMap.put(key, unitConfig);
            }
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

    /**
     * Generate a key for a label of a unit.
     * This key can only exists once.
     * Here the location id of the placement of the unit is added to the label to guarantee that this label
     * exists only once per location.
     * This method can be overwritten by sub classes to guarantee other things.
     *
     * @param label       the label for which the key is generated
     * @param languageKey the language key of the label.
     * @param unitConfig  the unit having the label
     *
     * @return a key for this label and unit
     */
    protected String generateKey(final String label, final String languageKey, final UnitConfig unitConfig) {
        return label + "_" + languageKey + "_" + unitConfig.getPlacementConfig().getLocationId();
    }

    @Override
    public void reset() {
        unitMap.clear();
    }
}
