package org.openbase.bco.registry.clazz.core.consistency;

/*
 * #%L
 * BCO Registry Class Core
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
import org.openbase.jul.extension.rst.processing.LabelProcessor;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import rst.domotic.unit.UnitTemplateConfigType.UnitTemplateConfig;
import rst.domotic.unit.device.DeviceClassType.DeviceClass;

import java.util.Locale;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Marian Pohling</a>
 */
public class UnitTemplateConfigLabelConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, DeviceClass, DeviceClass.Builder> {

    @Override
    public void processData(String id, IdentifiableMessage<String, DeviceClass, DeviceClass.Builder> entry, ProtoBufMessageMap<String, DeviceClass, DeviceClass.Builder> entryMap, ProtoBufRegistry<String, DeviceClass, DeviceClass.Builder> registry) throws CouldNotPerformException, EntryModification {
        DeviceClass.Builder deviceClass = entry.getMessage().toBuilder();

        deviceClass.clearUnitTemplateConfig();
        boolean modification = false;
        for (UnitTemplateConfig.Builder unitTemplateConfig : entry.getMessage().toBuilder().getUnitTemplateConfigBuilderList()) {
            if (!unitTemplateConfig.hasLabel()) {
                // unit template starts with deviceClassId_ so remove that
                LabelProcessor.addLabel(unitTemplateConfig.getLabelBuilder(), Locale.ENGLISH,
                        unitTemplateConfig.getId().substring(deviceClass.getId().length() + 1));
                modification = true;
            }
            deviceClass.addUnitTemplateConfig(unitTemplateConfig);
        }

        if (modification) {
            throw new EntryModification(entry.setMessage(deviceClass), this);
        }
    }
}
