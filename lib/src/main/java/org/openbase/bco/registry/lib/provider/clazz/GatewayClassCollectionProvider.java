package org.openbase.bco.registry.lib.provider.clazz;

/*-
 * #%L
 * BCO Registry Lib
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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

import org.openbase.jul.annotation.RPCMethod;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.type.domotic.unit.device.DeviceClassType.DeviceClass;
import org.openbase.type.domotic.unit.gateway.GatewayClassType.GatewayClass;

import java.util.List;

public interface GatewayClassCollectionProvider {

    /**
     * Method returns all registered gateway classes.
     *
     * @return the gateway classes stored in this registry.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    List<GatewayClass> getGatewayClasses() throws CouldNotPerformException;

    /**
     * Method returns true if a gateway class with the given id is
     * registered, otherwise false.
     *
     * @param gatewayClassId the id of the gateway class
     * @return true if a gateway class with the given id is registered, otherwise false
     */
    @RPCMethod
    Boolean containsGatewayClassById(final String gatewayClassId);


    /**
     * Method returns true if the gateway class with the given id is
     * registered, otherwise false. The gateway class id field is used for the
     * comparison.
     *
     * @param gatewayClass the gateway class which is tested
     * @return true if a gateway class with the given id is registered, otherwise false
     */
    @RPCMethod
    Boolean containsGatewayClass(final GatewayClass gatewayClass);

    /**
     * Method returns the gateway class which is registered with the given
     * id.
     *
     * @param gatewayClassId the id of the gateway class
     * @return the requested gateway class.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    @RPCMethod
    GatewayClass getGatewayClassById(final String gatewayClassId) throws CouldNotPerformException;
}
