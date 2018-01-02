package org.openbase.bco.manager.device.binding.openhab.util.configgen.items;

/*
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
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.processing.StringProcessor;
import rst.domotic.service.ServiceDescriptionType.ServiceDescription;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class LocationItemEntry extends AbstractItemEntry {

    public static String LOCATION_RSB_BINDING_CONFIG = "bco.manager.location";

    public LocationItemEntry(final UnitConfig locationUnitConfig, final ServiceDescription serviceDescription) throws org.openbase.jul.exception.InstantiationException {
        super(locationUnitConfig, null);
        try {
            this.itemId = generateItemId(locationUnitConfig, serviceDescription.getType());
            this.icon = "";
            this.commandType = getDefaultCommand(serviceDescription.getType());
            this.label = locationUnitConfig.getLabel() + "_" + StringProcessor.transformUpperCaseToCamelCase(serviceDescription.getType().name());
            if ("Number".equals(commandType)) {
                label += " [%.0f]";
            }
            this.itemHardwareConfig = "rsb=\"" + LOCATION_RSB_BINDING_CONFIG + ":" + locationUnitConfig.getId() + "\"";
            groups.add(ItemIdGenerator.generateUnitGroupID(UnitType.LOCATION));
            groups.add(ItemIdGenerator.generateUnitGroupID(locationUnitConfig));
            calculateGaps();
        } catch (CouldNotPerformException ex) {
            throw new org.openbase.jul.exception.InstantiationException(this, ex);
        }
    }

    private String generateItemId(final UnitConfig locationConfig, ServiceType serviceType) throws CouldNotPerformException {
        return StringProcessor.transformToIdString("Location")
                + ITEM_SEGMENT_DELIMITER
                + StringProcessor.transformUpperCaseToCamelCase(serviceType.name())
                + ITEM_SEGMENT_DELIMITER
                + ScopeGenerator.generateStringRepWithDelimiter(locationConfig.getScope(), ITEM_SUBSEGMENT_DELIMITER);
    }
}
