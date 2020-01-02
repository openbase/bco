package org.openbase.bco.registry.lib.provider.clazz;

/*-
 * #%L
 * BCO Registry Lib
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
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

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.annotation.RPCMethod;
import org.openbase.type.domotic.unit.device.DeviceClassType.DeviceClass;

import java.util.List;

public interface DeviceClassCollectionProvider {

    /**
     * Method returns all registered device classes.
     *
     * @return the device classes stored in this registry.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    List<DeviceClass> getDeviceClasses() throws CouldNotPerformException;

    /**
     * Method returns true if a device class with the given id is
     * registered, otherwise false.
     *
     * @param deviceClassId the id of the device class
     * @return true if a device class with the given id is registered, otherwise false
     */
    @RPCMethod
    Boolean containsDeviceClassById(final String deviceClassId);


    /**
     * Method returns true if the device class with the given id is
     * registered, otherwise false. The device class id field is used for the
     * comparison.
     *
     * @param deviceClass the device class which is tested
     * @return true if a device class with the given id is registered, otherwise false
     */
    @RPCMethod
    Boolean containsDeviceClass(final DeviceClass deviceClass);

    /**
     * Method returns the device class which is registered with the given
     * id.
     *
     * @param deviceClassId the id of the device class
     * @return the requested device class.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    @RPCMethod
    DeviceClass getDeviceClassById(final String deviceClassId) throws CouldNotPerformException;
}
