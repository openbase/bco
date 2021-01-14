package org.openbase.bco.registry.clazz.core.consistency;

/*-
 * #%L
 * BCO Registry Class Core
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

import org.openbase.bco.registry.template.remote.CachedTemplateRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.type.domotic.service.ServiceDescriptionType.ServiceDescription;
import org.openbase.type.domotic.service.ServiceTemplateConfigType.ServiceTemplateConfig;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.unit.UnitTemplateConfigType.UnitTemplateConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate;
import org.openbase.type.domotic.unit.device.DeviceClassType.DeviceClass;
import org.openbase.type.domotic.unit.device.DeviceClassType.DeviceClass.Builder;

import java.util.*;

/**
 * Consistency handler managing unit template configs in device classes. It generates an id and label if the
 * unit template config does not have them. Additionally it is validated that the service templates match
 * the service types defined in the unit template.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class DeviceClassUnitTemplateConfigConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, DeviceClass, Builder> {

    private final Map<String, UnitTemplateConfig> unitTemplateMap;

    public DeviceClassUnitTemplateConfigConsistencyHandler() {
        this.unitTemplateMap = new TreeMap<>();
    }

    @Override
    public void processData(final String id, final IdentifiableMessage<String, DeviceClass, Builder> entry, final ProtoBufMessageMap<String, DeviceClass, Builder> entryMap, final ProtoBufRegistry<String, DeviceClass, Builder> registry) throws CouldNotPerformException, EntryModification {
        final DeviceClass.Builder deviceClass = entry.getMessage().toBuilder();

        // remove all unit template configs
        deviceClass.clearUnitTemplateConfig();
        // save if a modification occurred
        boolean modification = false;
        // iterate over all unit template configs the device class has
        for (final UnitTemplateConfig.Builder unitTemplateConfig : entry.getMessage().toBuilder().getUnitTemplateConfigBuilderList()) {
            // if it does not have an id generate one
            if (!unitTemplateConfig.hasId() || unitTemplateConfig.getId().isEmpty()) {
                unitTemplateConfig.setId(generateUnitTemplateConfigId(deviceClass.getId(), unitTemplateConfig));
                modification = true;
            }
            // add it to map so that the next generated id is unique
            unitTemplateMap.put(unitTemplateConfig.getId(), unitTemplateConfig.build());

            // if the unit template config does not have a label generate one from its id
            if (!unitTemplateConfig.hasLabel() || LabelProcessor.isEmpty(unitTemplateConfig.getLabel())) {
                // unit template starts with deviceClassId_ so remove that
                LabelProcessor.addLabel(unitTemplateConfig.getLabelBuilder(), Locale.ENGLISH,
                        unitTemplateConfig.getId().substring(deviceClass.getId().length() + 1));
                modification = true;
            }

            // generate a set of service types provided by the unit template
            final UnitTemplate unitTemplate = CachedTemplateRegistryRemote.getRegistry().getUnitTemplateByType(unitTemplateConfig.getUnitType());
            final Set<ServiceType> unitTemplateServiceTypeSet = new HashSet<>();
            for (final ServiceDescription serviceDescription : unitTemplate.getServiceDescriptionList()) {
                unitTemplateServiceTypeSet.add(serviceDescription.getServiceType());
            }

            // remove all service template configs that do not match any service types in the unit template
            List<ServiceTemplateConfig> serviceTemplateConfigs = new ArrayList<>(unitTemplateConfig.getServiceTemplateConfigList());
            unitTemplateConfig.clearServiceTemplateConfig();
            for (final ServiceTemplateConfig serviceTemplateConfig : serviceTemplateConfigs) {
                if (!unitTemplateServiceTypeSet.contains(serviceTemplateConfig.getServiceType())) {
                    modification = true;
                    continue;
                }

                unitTemplateConfig.addServiceTemplateConfig(serviceTemplateConfig);
            }

            // generate a set of service types provided by the unit template config
            final Set<ServiceType> unitTemplateConfigServiceTypeSet = new HashSet<>();
            for (ServiceTemplateConfig serviceTemplateConfig : unitTemplateConfig.getServiceTemplateConfigList()) {
                unitTemplateConfigServiceTypeSet.add(serviceTemplateConfig.getServiceType());
            }

            // add a service template for every service type defined by the unit template and not contained in the unit template config
            for (ServiceType serviceType : unitTemplateServiceTypeSet) {
                if (unitTemplateConfigServiceTypeSet.contains(serviceType)) {
                    continue;
                }

                unitTemplateConfig.addServiceTemplateConfigBuilder().setServiceType(serviceType);
                modification = true;
            }

            // re-add the unit template config
            deviceClass.addUnitTemplateConfig(unitTemplateConfig);
        }

        if (modification) {
            throw new EntryModification(entry.setMessage(deviceClass, this), this);
        }
    }

    /**
     * Generate an id for a unit template config. The id will have the form deviceClassId_unitType_number where number
     * is the lowest number available for the unit type. E.g. the number will start with 0 and if there is another unit
     * template config with the same unit type it will get the number 1.
     *
     * @param deviceClassId      the id of the device class the template belongs to.
     * @param unitTemplateConfig the template for which an id is generated.
     *
     * @return an id for the template generated as described above.
     */
    private String generateUnitTemplateConfigId(final String deviceClassId, final UnitTemplateConfig.Builder unitTemplateConfig) {
        int number = 0;
        String unitConfigTemplateTypeId = deviceClassId + "_" + unitTemplateConfig.getUnitType().name() + "_" + number;
        while (unitTemplateMap.containsKey(unitConfigTemplateTypeId)) {
            number++;
            unitConfigTemplateTypeId = deviceClassId + "_" + unitTemplateConfig.getUnitType().name() + "_" + number;
        }
        return unitConfigTemplateTypeId;
    }

    @Override
    public void reset() {
        unitTemplateMap.clear();
    }
}
