package org.openbase.bco.registry.lib.provider.template;

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
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.ArrayList;
import java.util.List;

public interface UnitTemplateCollectionProvider {

    /**
     * Method returns true if the unit template with the given id is
     * registered, otherwise false. The unit template id field is used for the
     * comparison.
     *
     * Note: Method returns true in case the registry is not available. Maybe you need to check this in advance.
     *
     * @param unitTemplate the unit template which is tested
     * @return if the unit template with the given id is registered, otherwise false
     */
    @RPCMethod
    Boolean containsUnitTemplate(final UnitTemplate unitTemplate);

    /**
     * Method returns true if the unit template with the given id is
     * registered, otherwise false.
     *
     * Note: Method returns true in case the registry is not available. Maybe you need to check this in advance.
     *
     * @param unitTemplateId the id of the unit template
     * @return if the unit template with the given id is registered, otherwise false
     */
    @RPCMethod
    Boolean containsUnitTemplateById(final String unitTemplateId);

    /**
     * Method returns the unit template which is registered with the given
     * id.
     *
     * @param unitTemplateId the id of the unit template
     * @return the requested unit template.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    @RPCMethod
    UnitTemplate getUnitTemplateById(final String unitTemplateId) throws CouldNotPerformException;

    /**
     * Method returns all registered unit template.
     *
     * @return the unit templates stored in this registry.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    List<UnitTemplate> getUnitTemplates() throws CouldNotPerformException;

    /**
     * Method returns the unit template with the given type.
     *
     * @param unitType the unit type
     * @return the requested unit template.
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    @RPCMethod
    UnitTemplate getUnitTemplateByType(final UnitType unitType) throws CouldNotPerformException;

    /**
     * Get all sub types of a unit type. E.g. COLORABLE_LIGHT and DIMMABLE_LIGHT are
     * sub types of LIGHT.
     *
     * @param type the super type whose sub types are searched
     * @return all types of which the given type is a super type
     * @throws CouldNotPerformException
     */
    default List<UnitType> getSubUnitTypes(final UnitType type) throws CouldNotPerformException {
        List<UnitTemplate.UnitType> unitTypes = new ArrayList<>();
        for (UnitTemplate template : getUnitTemplates()) {
            if (template.getSuperTypeList().contains(type)) {
                unitTypes.add(template.getUnitType());
                unitTypes.addAll(getSubUnitTypes(template.getUnitType()));
            }
        }
        return unitTypes;
    }

    /**
     * Get all super types of a unit type. E.g. DIMMABLE_LIGHT and LIGHT are
     * super types of COLORABLE_LIGHT.
     *
     * @param type the type whose super types are returned
     * @return all super types of a given unit type
     * @throws CouldNotPerformException
     */
    default List<UnitType> getSuperUnitTypes(final UnitType type) throws CouldNotPerformException {
        UnitTemplate unitTemplate = getUnitTemplateByType(type);
        List<UnitType> unitTypes = new ArrayList<>();
        for (UnitTemplate template : getUnitTemplates()) {
            if (unitTemplate.getSuperTypeList().contains(template.getUnitType())) {
                unitTypes.add(template.getUnitType());
                unitTypes.addAll(getSuperUnitTypes(template.getUnitType()));
            }
        }
        return unitTypes;
    }
}
