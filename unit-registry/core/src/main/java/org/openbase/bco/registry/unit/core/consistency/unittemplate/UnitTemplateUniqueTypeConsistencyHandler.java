package org.openbase.bco.registry.unit.core.consistency.unittemplate;

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
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import rst.domotic.unit.UnitTemplateType.UnitTemplate;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.Builder;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.HashMap;
import java.util.Map;

/**
 * Consistency handler which makes sure that per unit type only one template is registered.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class UnitTemplateUniqueTypeConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitTemplate, Builder> {

    final Map<UnitType, UnitTemplate> unitTypeUnitTemplateMap;

    public UnitTemplateUniqueTypeConsistencyHandler() {
        this.unitTypeUnitTemplateMap = new HashMap<>();
    }

    @Override
    public void processData(final String id,
                            final IdentifiableMessage<String, UnitTemplate, Builder> entry,
                            final ProtoBufMessageMap<String, UnitTemplate, Builder> entryMap, ProtoBufRegistry<String, UnitTemplate, Builder> registry)
            throws CouldNotPerformException, EntryModification {

        final UnitTemplate unitTemplate = entry.getMessage();
        if (unitTypeUnitTemplateMap.containsKey(unitTemplate.getType()) && !unitTemplate.getId().equals(unitTypeUnitTemplateMap.get(unitTemplate.getType()).getId())) {
            throw new VerificationFailedException("UnitTemplate[" + unitTypeUnitTemplateMap.get(unitTemplate.getType()) + "] and unitTemplate[" + unitTemplate + "] both contain the same type");
        }

        unitTypeUnitTemplateMap.put(unitTemplate.getType(), unitTemplate);
    }

    @Override
    public void reset() {
        unitTypeUnitTemplateMap.clear();
    }
}
