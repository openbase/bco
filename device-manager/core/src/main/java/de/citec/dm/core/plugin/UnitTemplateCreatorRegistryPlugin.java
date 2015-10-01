/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dm.core.plugin;

import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import de.citec.jul.storage.registry.RegistryInterface;
import de.citec.jul.storage.registry.plugin.FileRegistryPluginAdapter;
import rst.homeautomation.device.DeviceRegistryType;
import rst.homeautomation.unit.UnitTemplateType;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 */
public class UnitTemplateCreatorRegistryPlugin extends FileRegistryPluginAdapter {

    private ProtoBufFileSynchronizedRegistry<String, UnitTemplateType.UnitTemplate, UnitTemplateType.UnitTemplate.Builder, DeviceRegistryType.DeviceRegistry.Builder> registry;

    public UnitTemplateCreatorRegistryPlugin(ProtoBufFileSynchronizedRegistry<String, UnitTemplateType.UnitTemplate, UnitTemplateType.UnitTemplate.Builder, DeviceRegistryType.DeviceRegistry.Builder> unitTemplateRegistry) {
        this.registry = unitTemplateRegistry;
    }

    @Override
    public void init(RegistryInterface reg) throws CouldNotPerformException {
        try {
            String templateId;
            UnitTemplate template;

            // create missing unit template
            if (registry.size() <= UnitTemplate.UnitType.values().length -1) {
                for (UnitType unitType : UnitType.values()) {
                    if(unitType == UnitType.UNKNOWN) {
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
            throw new CouldNotPerformException("Could not init " + getClass().getSimpleName() + "!", ex);
        }
    }
}
