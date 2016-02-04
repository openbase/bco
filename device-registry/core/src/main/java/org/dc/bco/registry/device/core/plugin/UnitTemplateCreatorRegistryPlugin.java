/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.device.core.plugin;

/*
 * #%L
 * REM DeviceRegistry Core
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
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
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.dc.jul.storage.registry.Registry;
import org.dc.jul.storage.registry.plugin.FileRegistryPluginAdapter;
import rst.homeautomation.device.DeviceRegistryType;
import rst.homeautomation.device.DeviceRegistryType.DeviceRegistry;
import rst.homeautomation.unit.UnitTemplateType;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 */
public class UnitTemplateCreatorRegistryPlugin extends FileRegistryPluginAdapter<String, IdentifiableMessage<String, UnitTemplateType.UnitTemplate, UnitTemplateType.UnitTemplate.Builder>> {

    private ProtoBufFileSynchronizedRegistry<String, UnitTemplate, UnitTemplate.Builder, DeviceRegistry.Builder> registry;

    public UnitTemplateCreatorRegistryPlugin(ProtoBufFileSynchronizedRegistry<String, UnitTemplateType.UnitTemplate, UnitTemplateType.UnitTemplate.Builder, DeviceRegistryType.DeviceRegistry.Builder> unitTemplateRegistry) {
        this.registry = unitTemplateRegistry;
    }

    @Override
    public void init(Registry<String, IdentifiableMessage<String, UnitTemplateType.UnitTemplate, UnitTemplateType.UnitTemplate.Builder>, ?> config) throws InitializationException, InterruptedException {
        try {
            String templateId;
            UnitTemplate template;

            // create missing unit template
            if (registry.size() <= UnitTemplate.UnitType.values().length - 1) {
                for (UnitType unitType : UnitType.values()) {
                    if (unitType == UnitType.UNKNOWN) {
                        continue;
                    }
                    template = UnitTemplate.newBuilder().setType(unitType).build();
                    templateId = registry.getIdGenerator().generateId(template);
                    if (!registry.contains(templateId)) {
                        registry.register(template);
                    }
                }
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException("Could not init " + getClass().getSimpleName() + "!", ex);
        }
    }
}
