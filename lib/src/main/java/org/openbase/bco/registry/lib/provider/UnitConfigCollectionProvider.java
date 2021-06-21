package org.openbase.bco.registry.lib.provider;

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
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.List;

public interface UnitConfigCollectionProvider {

    /**
     * Method returns all non-disabled registered unit configs.
     *
     * @return the not disabled unit configs stored in this registry.
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    default List<UnitConfig> getUnitConfigs() throws CouldNotPerformException {
        return getUnitConfigsFiltered(true);
    }

    /**
     * Method returns all registered unit configs. It allows to filter disabled unit configs.
     *
     * @param filterDisabledUnits if true all unit configs which are disabled will be skipped.
     *
     * @return the unit configs stored in this registry.
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    List<UnitConfig> getUnitConfigsFiltered(final boolean filterDisabledUnits) throws CouldNotPerformException;

    /**
     * Method returns true if the unit config with the given id is
     * registered, otherwise false.
     *
     * @param unitConfigId the id to identify the unit.
     *
     * @return true if the unit exists.
     */
    @RPCMethod
    Boolean containsUnitConfigById(final String unitConfigId);

    /**
     * Method returns the unit config which is registered with the given
     * unit id.
     *
     * @param unitConfigId
     *
     * @return the requested unit config.
     *
     * @throws NotAvailableException is thrown if the request fails.
     */
    @RPCMethod
    UnitConfig getUnitConfigById(final String unitConfigId) throws NotAvailableException;

    /**
     * Method returns the unit config which is registered with the given
     * unit id. Additionally the type will be verified.
     *
     * @param unitConfigId the identifier of the unit.
     * @param unitType     the type to verify.
     *
     * @return the requested unit config validated with the given unit type.
     *
     * @throws NotAvailableException is thrown if the request fails.
     @deprecated since 2.0 and will be removed in 3.0: please use getUnitConfigByIdAndUnitType(...) instead.
     */
    @RPCMethod
    @Deprecated
    default UnitConfig getUnitConfigById(final String unitConfigId, final UnitType unitType) throws NotAvailableException {
        final UnitConfig unitConfig = getUnitConfigById(unitConfigId);

        try {
            // validate type
            if (unitType != UnitType.UNKNOWN && unitConfig.getUnitType() != unitType) {
                throw new VerificationFailedException("Referred Unit[" + LabelProcessor.getBestMatch(unitConfig.getLabel()) + "] is not compatible to given type!");
            }
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("UnitConfigId", unitConfigId, ex);
        }

        return unitConfig;
    }

    /**
     * Method returns the unit config which is registered with the given
     * unit id. Additionally the type will be verified.
     *
     * @param unitConfigId the identifier of the unit.
     * @param unitType     the type to verify.
     *
     * @return the requested unit config validated with the given unit type.
     *
     * @throws NotAvailableException is thrown if the request fails.
     */
    default UnitConfig getUnitConfigByIdAndUnitType(final String unitConfigId, final UnitType unitType) throws NotAvailableException {
        final UnitConfig unitConfig = getUnitConfigById(unitConfigId);

        try {
            // validate type
            if (unitType != UnitType.UNKNOWN && unitConfig.getUnitType() != unitType) {
                throw new VerificationFailedException("Referred Unit[" + LabelProcessor.getBestMatch(unitConfig.getLabel()) + "] is not compatible to given type!");
            }
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("UnitConfigId", unitConfigId, ex);
        }

        return unitConfig;
    }

    /**
     * Method returns a list of all globally registered units of the given {@code type}.
     * <p>
     * Note: The type {@code UnitType.UNKNOWN} is used as wildcard and will return a list of all registered units.
     *
     * @param type the unit type to filter.
     *
     * @return a list of unit configurations.
     *
     * @throws CouldNotPerformException is thrown in case something goes wrong during the request.
     */
    List<UnitConfig> getUnitConfigsByUnitType(final UnitType type) throws CouldNotPerformException;
}
