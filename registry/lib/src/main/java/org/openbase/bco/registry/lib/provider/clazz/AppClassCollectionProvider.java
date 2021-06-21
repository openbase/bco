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

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.annotation.RPCMethod;
import org.openbase.type.domotic.unit.app.AppClassType.AppClass;

import java.util.List;

public interface AppClassCollectionProvider {

    /**
     * Method returns true if a app class with the given id is
     * registered, otherwise false.
     *
     * @param appClassId the id of the app class
     * @return true if a app class with the given id is registered, otherwise false
     * @throws CouldNotPerformException is thrown if the check fails.
     */
    @RPCMethod
    Boolean containsAppClassById(String appClassId) throws CouldNotPerformException;


    /**
     * Method returns true if the app class with the given id is
     * registered, otherwise false. The app class id field is used for the
     * comparison.
     *
     * @param appClass the app class which is tested
     * @return true if a app class with the given id is registered, otherwise false
     * @throws CouldNotPerformException is thrown if the check fails.
     */
    @RPCMethod
    Boolean containsAppClass(AppClass appClass) throws CouldNotPerformException;

    /**
     * Method returns all registered app classes.
     *
     * @return the app classes stored in this registry.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    List<AppClass> getAppClasses() throws CouldNotPerformException;

    /**
     * Method returns the app class which is registered with the given
     * id.
     *
     * @param appClassId the id of the app class
     * @return the requested app class.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    @RPCMethod
    AppClass getAppClassById(final String appClassId) throws CouldNotPerformException;
}
