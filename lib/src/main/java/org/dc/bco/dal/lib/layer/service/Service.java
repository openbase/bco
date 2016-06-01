package org.dc.bco.dal.lib.layer.service;

import org.dc.bco.dal.lib.layer.service.consumer.ConsumerService;
import org.dc.bco.dal.lib.layer.service.operation.OperationService;
import org.dc.bco.dal.lib.layer.service.provider.ProviderService;
import org.dc.jul.processing.StringProcessor;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate;

/*
 * #%L
 * DAL Library
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

/**
 *
 * @author Divine Threepwood
 */
public interface Service {

    public static final String SERVICE_LABEL = Service.class.getSimpleName();
    public static final String PROVIDER_SERVICE_LABEL = ProviderService.class.getSimpleName();
    public static final String CONSUMER_SERVICE_LABEL = ConsumerService.class.getSimpleName();
    public static final String OPERATION_SERVICE_LABEL = OperationService.class.getSimpleName();

    /**
     * This method returns the service base name of the given service type.
     * 
     * The base name is the service name without service suffix. 
     * e.g. PowerStateProviderService -> PowerState
     * 
     * @param serviceType the service type to extract the base name.
     * @return the service base name.
     */
    public static String getServiceBaseName(ServiceTemplate.ServiceType serviceType) {
        return StringProcessor.transformUpperCaseToCamelCase(serviceType.name()).replaceAll(Service.PROVIDER_SERVICE_LABEL, "").replaceAll(Service.CONSUMER_SERVICE_LABEL, "").replaceAll(Service.OPERATION_SERVICE_LABEL, "");
    }
    
    
}
