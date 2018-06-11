package org.openbase.bco.app.cloud.connector.mapping.unit;

/*-
 * #%L
 * BCO Cloud Connector
 * %%
 * Copyright (C) 2018 openbase.org
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
                if (serviceTypeSet.contains(serviceDescription.getServiceType())) {
                    continue;
                }
                serviceTypeSet.add(serviceDescription.getServiceType());

                // find traits for the given service type
                ServiceTypeTraitMapping byServiceType = ServiceTypeTraitMapping.getByServiceType(serviceDescription.getServiceType());
                for (Trait trait : byServiceType.getTraitSet()) {
                    try {
                        // map service state to trait
                        trait.getTraitMapper().map((GeneratedMessage) Services.invokeProviderServiceMethod(serviceDescription.getServiceType(), unitRemote), deviceState);
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
