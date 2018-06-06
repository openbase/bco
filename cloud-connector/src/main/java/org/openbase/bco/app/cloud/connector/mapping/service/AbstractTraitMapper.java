package org.openbase.bco.app.cloud.connector.mapping.service;

import com.google.gson.JsonObject;
import com.google.protobuf.GeneratedMessage;
import org.openbase.bco.app.cloud.connector.mapping.lib.Command;
import org.openbase.jul.exception.CouldNotPerformException;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public abstract class AbstractTraitMapper<SERVICE_STATE extends GeneratedMessage> implements TraitMapper<SERVICE_STATE> {

    private final ServiceType serviceType;

    public AbstractTraitMapper(ServiceType serviceType) {
        this.serviceType = serviceType;
    }

    @Override
    public SERVICE_STATE map(JsonObject jsonObject, Command command) throws CouldNotPerformException {
        return map(jsonObject);
    }

    protected abstract SERVICE_STATE map(JsonObject jsonObject) throws CouldNotPerformException;

    @Override
    public void addAttributes(UnitConfig unitConfig, JsonObject jsonObject) {

    }

    @Override
    public ServiceType getServiceType() {
        return serviceType;
    }
}
