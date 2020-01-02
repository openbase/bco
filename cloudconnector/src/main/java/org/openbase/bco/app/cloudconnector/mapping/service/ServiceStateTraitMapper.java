package org.openbase.bco.app.cloudconnector.mapping.service;

/*-
 * #%L
 * BCO Cloud Connector
 * %%
 * Copyright (C) 2018 - 2020 openbase.org
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
import org.openbase.bco.app.cloudconnector.mapping.lib.Command;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

/**
 * Interface defining methods for mapping from a service state to a trait and back.
 *
 * @param <SERVICE_STATE> the service state this mapper handles. E.g. PowerState.
 */
public interface ServiceStateTraitMapper<SERVICE_STATE extends Message> {

    /**
     * Map from a trait to a service state depending on the command of the trait triggered.
     *
     * @param jsonObject the json object defining the trait value.
     * @param command    the command of the trait triggered.
     *
     * @return a mapped service state.
     *
     * @throws CouldNotPerformException if mapping fails.
     */
    SERVICE_STATE map(final JsonObject jsonObject, final Command command) throws CouldNotPerformException;

    /**
     * Map from a service state to a trait. The values for the trait have to be added
     * to the json object parameter.
     *
     * @param serviceState the service state mapped to a trait.
     * @param jsonObject   the json object to which values according to the mapped trait are added.
     *
     * @throws CouldNotPerformException if mapping fails.
     */
    void map(final SERVICE_STATE serviceState, final JsonObject jsonObject) throws CouldNotPerformException;

    /**
     * Get the service
     *
     * @return
     */
    ServiceType getServiceType();

    /**
     * Add attributes to the json object depending on the provided unit config. This is required for
     * configuring a trait. For example the color temperature trait requires a min and a max temperature.
     *
     * @param unitConfig the unit config of the unit for which the attributes have to be added.
     * @param jsonObject the json object to which the attributes are added.
     *
     * @throws CouldNotPerformException if resolving the attributes fails.
     */
    void addAttributes(final UnitConfig unitConfig, final JsonObject jsonObject) throws CouldNotPerformException;

}
