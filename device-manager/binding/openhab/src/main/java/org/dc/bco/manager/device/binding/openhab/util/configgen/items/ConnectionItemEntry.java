/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.device.binding.openhab.util.configgen.items;

import org.dc.bco.manager.device.binding.openhab.util.configgen.GroupEntry;
import static org.dc.bco.manager.device.binding.openhab.util.configgen.items.AbstractItemEntry.ITEM_SEGMENT_DELIMITER;
import static org.dc.bco.manager.device.binding.openhab.util.configgen.items.AbstractItemEntry.ITEM_SUBSEGMENT_DELIMITER;
import static org.dc.bco.manager.device.binding.openhab.util.configgen.items.LocationItemEntry.LOCATION_RSB_BINDING_CONFIG;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.extension.rsb.scope.ScopeGenerator;
import org.dc.jul.processing.StringProcessor;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.spatial.ConnectionConfigType.ConnectionConfig;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class ConnectionItemEntry extends AbstractItemEntry {

    public static String CONNECTION_GROUP_LABEL = "Connections";

    public ConnectionItemEntry(final ConnectionConfig connectionConfig, final ServiceType serviceType) throws org.dc.jul.exception.InstantiationException {
        super();
        try {
            this.itemId = generateItemId(connectionConfig, serviceType);
            this.icon = "";
            this.commandType = getDefaultCommand(serviceType);
            this.label = connectionConfig.getLabel();
            this.itemHardwareConfig = "rsb=\"" + LOCATION_RSB_BINDING_CONFIG + ":" + connectionConfig.getId() + "\"";
            groups.add(CONNECTION_GROUP_LABEL);
            groups.add(GroupEntry.generateGroupID(connectionConfig.getScope()));
            calculateGaps();
        } catch (CouldNotPerformException ex) {
            throw new org.dc.jul.exception.InstantiationException(this, ex);
        }
    }

    private String generateItemId(ConnectionConfig connectionConfig, ServiceType serviceType) throws CouldNotPerformException {
        return StringProcessor.transformToIdString("Connection")
                + ITEM_SEGMENT_DELIMITER
                + StringProcessor.transformUpperCaseToCamelCase(serviceType.name())
                + ITEM_SEGMENT_DELIMITER
                + ScopeGenerator.generateStringRepWithDelimiter(connectionConfig.getScope(), ITEM_SUBSEGMENT_DELIMITER);
    }
}
