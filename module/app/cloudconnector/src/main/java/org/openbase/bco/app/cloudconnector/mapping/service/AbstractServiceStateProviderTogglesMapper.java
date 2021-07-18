package org.openbase.bco.app.cloudconnector.mapping.service;

/*-
 * #%L
 * BCO Cloud Connector
 * %%
 * Copyright (C) 2018 - 2021 openbase.org
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
import com.google.protobuf.Message;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public abstract class AbstractServiceStateProviderTogglesMapper<SERVICE_STATE extends Message> extends AbstractServiceStateTogglesMapper<SERVICE_STATE> {

    public AbstractServiceStateProviderTogglesMapper(ServiceType serviceType) {
        super(serviceType);
    }

    @Override
    protected SERVICE_STATE map(JsonObject jsonObject) throws CouldNotPerformException {
        throw new CouldNotPerformException("Setting toggle not supported for serviceType[" + getServiceType().name() + "]");
    }

    @Override
    public SERVICE_STATE getServiceState(boolean on) throws CouldNotPerformException {
        throw new CouldNotPerformException("Provider service cannot map from[" + on + "] to service state");
    }
}
