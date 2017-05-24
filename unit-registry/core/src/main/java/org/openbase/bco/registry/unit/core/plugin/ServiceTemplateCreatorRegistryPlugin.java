package org.openbase.bco.registry.unit.core.plugin;

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.Registry;
import org.openbase.jul.storage.registry.plugin.FileRegistryPluginAdapter;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ServiceTemplateCreatorRegistryPlugin extends FileRegistryPluginAdapter<String, IdentifiableMessage<String, ServiceTemplate, ServiceTemplate.Builder>> {

    private final ProtoBufFileSynchronizedRegistry<String, ServiceTemplate, ServiceTemplate.Builder, UnitRegistryData.Builder> registry;

    public ServiceTemplateCreatorRegistryPlugin(ProtoBufFileSynchronizedRegistry<String, ServiceTemplate, ServiceTemplate.Builder, UnitRegistryData.Builder> unitTemplateRegistry) {
        this.registry = unitTemplateRegistry;
    }

    @Override
    public void init(Registry<String, IdentifiableMessage<String, ServiceTemplate, ServiceTemplate.Builder>> config) throws InitializationException, InterruptedException {
        try {
            ServiceTemplate template;

            // create missing unit template
            if (registry.size() <= ServiceType.values().length - 1) {
                for (ServiceType serviceType : ServiceType.values()) {
                    if (serviceType == ServiceType.UNKNOWN) {
                        continue;
                    }
                    template = ServiceTemplate.newBuilder().setType(serviceType).build();
                    if (!containsServiceTemplateByType(serviceType)) {
                        registry.register(template);
                    }
                }
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException("Could not init " + getClass().getSimpleName() + "!", ex);
        }
    }

    private boolean containsServiceTemplateByType(ServiceType type) throws CouldNotPerformException {
        for (ServiceTemplate serviceTemplate : registry.getMessages()) {
            if (serviceTemplate.getType() == type) {
                return true;
            }
        }
        return false;
    }
    
}
