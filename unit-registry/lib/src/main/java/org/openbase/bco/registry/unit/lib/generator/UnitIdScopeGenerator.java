package org.openbase.bco.registry.unit.lib.generator;

/*-
 * #%L
 * BCO Registry Unit Library
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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

import org.openbase.bco.registry.clazz.lib.ClassRegistry;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.extension.type.processing.ScopeProcessor;
import org.openbase.type.communication.ScopeType.Scope;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.agent.AgentClassType.AgentClass;

import java.util.Locale;

public class UnitIdScopeGenerator implements UnitScopeGenerator {

    @Override
    public Scope generateScope(final UnitConfig unitConfig, final UnitRegistry unitRegistry, final ClassRegistry classRegistry) throws CouldNotPerformException {

        if (unitRegistry == null) {
            throw new NotAvailableException("UnitRegistry");
        }

        if (classRegistry == null) {
            throw new NotAvailableException("ClassRegistry");
        }

        if (unitConfig == null) {
            throw new NotAvailableException("unitConfig");
        }

        if (!unitConfig.hasPlacementConfig()) {
            throw new NotAvailableException("location.placementConfig");
        }

        if (!unitConfig.getPlacementConfig().hasLocationId() || unitConfig.getPlacementConfig().getLocationId().isEmpty()) {
            throw new NotAvailableException("location.placementConfig.locationId");
        }


        switch (unitConfig.getUnitType()) {
            case LOCATION:
                return generateLocationScope(unitConfig, unitRegistry);
            default:
                return generateUnitScope(unitConfig, unitRegistry);
        }
    }

    private static Scope generateLocationScope(final UnitConfig unitConfig, final UnitRegistry unitRegistry) throws CouldNotPerformException {

        Scope.Builder scope = Scope.newBuilder();
        if (!unitConfig.getLocationConfig().getRoot()) {

            final UnitConfig locationConfig = unitRegistry.getUnitConfigByIdAndUnitType(unitConfig.getPlacementConfig().getLocationId(), UnitType.LOCATION);

            if (!locationConfig.hasScope() || locationConfig.getScope().getComponentList().isEmpty()) {
                throw new NotAvailableException("location scope");
            }

            scope.addAllComponent(locationConfig.getScope().getComponentList());
        }
        scope.addComponent(ScopeProcessor.convertIntoValidScopeComponent(getAlias(unitConfig)));

        return scope.build();
    }

    private static Scope generateUnitScope(final UnitConfig unitConfig, final UnitRegistry unitRegistry) throws CouldNotPerformException {

        final UnitConfig locationConfig = unitRegistry.getUnitConfigByIdAndUnitType(unitConfig.getPlacementConfig().getLocationId(), UnitType.LOCATION);

        if (!locationConfig.hasScope() || locationConfig.getScope().getComponentList().isEmpty()) {
            throw new NotAvailableException("location scope");
        }

        // add location scope
        Scope.Builder scope = locationConfig.getScope().toBuilder();

        // add unit type
        scope.addComponent(ScopeProcessor.convertIntoValidScopeComponent(unitConfig.getUnitType().name().replace("_", "")));

        // add unit alias
        scope.addComponent(ScopeProcessor.convertIntoValidScopeComponent(getAlias(unitConfig)));

        return scope.build();
    }

    private static String getAlias(final UnitConfig unitConfig) throws NotAvailableException {
        if (unitConfig.getAliasCount() == 0) {
            throw new NotAvailableException("unitConfig.alias");
        }

        return unitConfig.getAlias(0);
    }
}
