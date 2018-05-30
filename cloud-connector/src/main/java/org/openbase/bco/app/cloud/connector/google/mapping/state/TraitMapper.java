package org.openbase.bco.app.cloud.connector.google.mapping.state;

import com.google.gson.JsonObject;
import com.google.protobuf.GeneratedMessage;
import org.openbase.jul.exception.CouldNotPerformException;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.UnitConfigType.UnitConfig;

public interface TraitMapper<SERVICE_STATE extends GeneratedMessage> {

    SERVICE_STATE map(final JsonObject jsonObject) throws CouldNotPerformException;

    void map(final SERVICE_STATE serviceState, final JsonObject jsonObject) throws CouldNotPerformException;

    ServiceType getServiceType();

    default void addAttributes(final UnitConfig unitConfig, final JsonObject jsonObject) throws CouldNotPerformException {
        //do nothing, can be overwritten if needed
    }

}
