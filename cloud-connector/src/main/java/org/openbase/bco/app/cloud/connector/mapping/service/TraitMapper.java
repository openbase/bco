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
import com.google.protobuf.GeneratedMessage;
import org.openbase.bco.app.cloud.connector.mapping.lib.Command;
import org.openbase.jul.exception.CouldNotPerformException;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.UnitConfigType.UnitConfig;

public interface TraitMapper<SERVICE_STATE extends GeneratedMessage> {

    SERVICE_STATE map(final JsonObject jsonObject, final Command command) throws CouldNotPerformException;

    void map(final SERVICE_STATE serviceState, final JsonObject jsonObject) throws CouldNotPerformException;

    ServiceType getServiceType();

    void addAttributes(final UnitConfig unitConfig, final JsonObject jsonObject) throws CouldNotPerformException;

}
