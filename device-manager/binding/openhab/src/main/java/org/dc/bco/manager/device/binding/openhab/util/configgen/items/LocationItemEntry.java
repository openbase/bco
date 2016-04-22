/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.device.binding.openhab.util.configgen.items;

import org.dc.bco.manager.device.binding.openhab.util.configgen.GroupEntry;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.extension.rsb.scope.ScopeGenerator;
import org.dc.jul.processing.StringProcessor;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class LocationItemEntry extends AbstractItemEntry {

    public static String LOCATION_GROUP_LABEL = "Locations";
    public static String LOCATION_RSB_BINDING_CONFIG = "bco.manager.location";

    public LocationItemEntry(final LocationConfig locationConfig, final ServiceType serviceType) throws org.dc.jul.exception.InstantiationException {
        super();
        try {
            this.itemId = generateItemId(locationConfig, serviceType);
            this.icon = "";
            this.commandType = getDefaultCommand(serviceType);
            this.label = locationConfig.getLabel();
            this.itemHardwareConfig = "rsb=\"" + LOCATION_RSB_BINDING_CONFIG + ":" + locationConfig.getId() + "\"";
            groups.add(LOCATION_GROUP_LABEL);
            groups.add(GroupEntry.generateGroupID(locationConfig));
            calculateGaps();
        } catch (CouldNotPerformException ex) {
            throw new org.dc.jul.exception.InstantiationException(this, ex);
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
