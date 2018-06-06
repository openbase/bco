package org.openbase.bco.app.cloud.connector.mapping.unit;

import com.google.gson.JsonObject;
import com.google.protobuf.GeneratedMessage;
import org.openbase.bco.app.cloud.connector.FulfillmentHandler;
import org.openbase.bco.app.cloud.connector.mapping.lib.ErrorCode;
import org.openbase.bco.app.cloud.connector.mapping.service.ServiceTypeTraitMapping;
import org.openbase.bco.app.cloud.connector.mapping.lib.Trait;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import rst.domotic.service.ServiceDescriptionType.ServiceDescription;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class DefaultUnitTypeMapper<UR extends UnitRemote> implements UnitTypeMapper<UR> {

    @Override
    public void map(UR unitRemote, JsonObject deviceState) {
        //TODO: this can use getServiceTypes() the future
        // iterate over all services of the unit and skip duplicates
        final Set<ServiceType> serviceTypeSet = new HashSet<>();
        try {
            for (ServiceDescription serviceDescription : unitRemote.getUnitTemplate().getServiceDescriptionList()) {
                if (serviceTypeSet.contains(serviceDescription.getType())) {
                    continue;
                }
                serviceTypeSet.add(serviceDescription.getType());

                // find traits for the given service type
                ServiceTypeTraitMapping byServiceType = ServiceTypeTraitMapping.getByServiceType(serviceDescription.getType());
                for (Trait trait : byServiceType.getTraitSet()) {
                    try {
                        // map service state to trait
                        trait.getTraitMapper().map((GeneratedMessage) Services.invokeProviderServiceMethod(serviceDescription.getType(), unitRemote), deviceState);
                    } catch (CouldNotPerformException ex) {
                        // getting service value failed
                        FulfillmentHandler.setError(deviceState, ex, ErrorCode.UNKNOWN_ERROR);
                    }
                }
            }
        } catch (NotAvailableException ex) {
            // getUnitTemplate or get ServiceTypeTraitMapper failed
            FulfillmentHandler.setError(deviceState, ex, ErrorCode.UNKNOWN_ERROR);
        }
    }
}
