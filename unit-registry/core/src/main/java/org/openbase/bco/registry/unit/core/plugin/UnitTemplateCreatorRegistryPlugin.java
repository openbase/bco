package org.openbase.bco.registry.unit.core.plugin;

/*
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
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.jul.storage.registry.Registry;
import org.openbase.jul.storage.registry.plugin.FileRegistryPluginAdapter;
import org.openbase.jul.storage.registry.plugin.ProtobufRegistryPluginAdapter;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.unit.UnitTemplateType.UnitTemplate;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.Builder;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 *
 */
public class UnitTemplateCreatorRegistryPlugin extends ProtobufRegistryPluginAdapter<String, UnitTemplate, Builder> {

    private final ProtoBufFileSynchronizedRegistry<String, UnitTemplate, UnitTemplate.Builder, UnitRegistryData.Builder> registry;

    public UnitTemplateCreatorRegistryPlugin(ProtoBufFileSynchronizedRegistry<String, UnitTemplate, UnitTemplate.Builder, UnitRegistryData.Builder> unitTemplateRegistry) {
        this.registry = unitTemplateRegistry;
    }

    @Override
    public void init(ProtoBufRegistry<String, UnitTemplate, Builder> config) throws InitializationException, InterruptedException {
        try {
            UnitTemplate template;

            // create missing unit template
            if (registry.size() <= UnitTemplate.UnitType.values().length - 1) {
                for (UnitType unitType : UnitType.values()) {
                    if (unitType == UnitType.UNKNOWN) {
                        continue;
                    }
                    template = UnitTemplate.newBuilder().setType(unitType).build();
                    if (!containsUnitTemplateByType(unitType)) {
                        registry.register(template);
                    }
                }
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException("Could not init " + getClass().getSimpleName() + "!", ex);
        }
    }

    private boolean containsUnitTemplateByType(UnitType type) throws CouldNotPerformException {
        for (UnitTemplate unitTemplate : registry.getMessages()) {
            if (unitTemplate.getType() == type) {
                return true;
            }
        }
        return false;
    }
}
