package org.openbase.bco.manager.device.binding.openhab.util.configgen.items;

/*-
 * #%L
 * BCO Manager Device Binding OpenHAB
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
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

import static org.openbase.bco.manager.device.binding.openhab.util.configgen.GroupEntry.OPENHAB_ID_DELIMITER;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.slf4j.LoggerFactory;
import rst.domotic.service.ServiceTemplateType;
import rst.domotic.unit.UnitConfigType;
import rst.domotic.unit.UnitTemplateType;
import rst.rsb.ScopeType;
import rst.spatial.PlacementConfigType;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class ItemIdGenerator {
    
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ItemIdGenerator.class);

    public static String generateUnitGroupID(final UnitTemplateType.UnitTemplate.UnitType unitType) {
        return "bco_unit_" + unitType.name().toLowerCase();
    }
    
    public static String generateServiceGroupID(final ServiceTemplateType.ServiceTemplate.ServiceType serviceType) {
        return "bco_service_" + serviceType.name().toLowerCase();
    }
    
    public static String generateParentGroupID(final UnitConfigType.UnitConfig childLocationConfig) throws CouldNotPerformException, InterruptedException {

        try {
            return generateUnitGroupID(childLocationConfig.getPlacementConfig().getLocationId());
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could location parent id via placement config. Outdated registry entry!", ex), LOGGER);
            return generateUnitGroupID(childLocationConfig.getPlacementConfig().getLocationId());
        }
    }

    public static String generateUnitGroupID(final PlacementConfigType.PlacementConfig placementConfig) throws CouldNotPerformException, InterruptedException {
        String locationID;
        try {
            if (!placementConfig.hasLocationId()) {
                throw new NotAvailableException("placementconfig.locationid");
            }
            locationID = placementConfig.getLocationId();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not generate group id!", ex);
        }
        return generateUnitGroupID(locationID);
    }

    public static String generateUnitGroupID(final String unitId) throws CouldNotPerformException, InterruptedException {
        UnitConfigType.UnitConfig locationUnitConfig;
        try {
            locationUnitConfig = Registries.getUnitRegistry(true).getUnitConfigById(unitId);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not generate group id for LocationId[" + unitId + "]!", ex);
        }
        return generateUnitGroupID(locationUnitConfig);
    }

    public static String generateUnitGroupID(final UnitConfigType.UnitConfig config) throws CouldNotPerformException {
        try {
            if (!config.hasScope()) {
                throw new NotAvailableException("locationconfig.scope");
            }
            return generateUnitGroupID(config.getScope());
        } catch (NotAvailableException ex) {
            throw new CouldNotPerformException("Could not generate group id out of LocationConfig[" + config.getId() + "] !", ex);
        }
    }

    public static String generateUnitGroupID(final ScopeType.Scope scope) throws CouldNotPerformException {
        try {
            if (scope == null) {
                throw new NotAvailableException("locationconfig.scope");
            }
            return ScopeGenerator.generateStringRepWithDelimiter(scope, OPENHAB_ID_DELIMITER);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not generate group id out of Scope[" + scope + "]!", ex);
        }
    }
}
