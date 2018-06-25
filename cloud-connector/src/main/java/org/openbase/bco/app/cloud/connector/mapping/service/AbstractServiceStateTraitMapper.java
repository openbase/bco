package org.openbase.bco.app.cloud.connector.mapping.service;

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
import com.google.protobuf.Message;
import org.openbase.bco.app.cloud.connector.mapping.lib.Command;
import org.openbase.jul.exception.CouldNotPerformException;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public abstract class AbstractServiceStateTraitMapper<SERVICE_STATE extends Message> implements ServiceStateTraitMapper<SERVICE_STATE> {

    private final ServiceType serviceType;

    public AbstractServiceStateTraitMapper(ServiceType serviceType) {
        this.serviceType = serviceType;
    }

    @Override
    public SERVICE_STATE map(JsonObject jsonObject, Command command) throws CouldNotPerformException {
        return map(jsonObject);
    }

    protected abstract SERVICE_STATE map(JsonObject jsonObject) throws CouldNotPerformException;

    @Override
    public void addAttributes(UnitConfig unitConfig, JsonObject jsonObject) throws CouldNotPerformException {

    }

    @Override
    public ServiceType getServiceType() {
        return serviceType;
    }
}
