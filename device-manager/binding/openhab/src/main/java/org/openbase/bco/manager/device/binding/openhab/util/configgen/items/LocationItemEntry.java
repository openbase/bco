package org.openbase.bco.manager.device.binding.openhab.util.configgen.items;

/*
 * #%L
 * COMA DeviceManager Binding OpenHAB
 * %%
 * Copyright (C) 2015 - 2016 openbase.org
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

import org.openbase.bco.manager.device.binding.openhab.util.configgen.GroupEntry;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.processing.StringProcessor;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class LocationItemEntry extends AbstractItemEntry {

    public static String LOCATION_GROUP_LABEL = "Locations";
    public static String LOCATION_RSB_BINDING_CONFIG = "bco.manager.location";

    public LocationItemEntry(final LocationConfig locationConfig, final ServiceType serviceType) throws org.openbase.jul.exception.InstantiationException {
        super();
        try {
            this.itemId = generateItemId(locationConfig, serviceType);
            this.icon = "";
            this.commandType = getDefaultCommand(serviceType);
            this.label = locationConfig.getLabel() + "_" + StringProcessor.transformUpperCaseToCamelCase(serviceType.name());
            if("Number".equals(commandType)) {
                label += " [%.0f]";
            }
            this.itemHardwareConfig = "rsb=\"" + LOCATION_RSB_BINDING_CONFIG + ":" + locationConfig.getId() + "\"";
            groups.add(LOCATION_GROUP_LABEL);
            groups.add(GroupEntry.generateGroupID(locationConfig));
            calculateGaps();
        } catch (CouldNotPerformException ex) {
            throw new org.openbase.jul.exception.InstantiationException(this, ex);
        }
    }

    private String generateItemId(LocationConfig locationConfig, ServiceType serviceType) throws CouldNotPerformException {
        return StringProcessor.transformToIdString("Location")
                + ITEM_SEGMENT_DELIMITER
                + StringProcessor.transformUpperCaseToCamelCase(serviceType.name())
                + ITEM_SEGMENT_DELIMITER
                + ScopeGenerator.generateStringRepWithDelimiter(locationConfig.getScope(), ITEM_SUBSEGMENT_DELIMITER);
    }
}
