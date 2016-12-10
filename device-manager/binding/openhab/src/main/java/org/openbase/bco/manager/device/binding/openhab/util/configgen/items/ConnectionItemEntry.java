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
import static org.openbase.bco.manager.device.binding.openhab.util.configgen.items.AbstractItemEntry.ITEM_SEGMENT_DELIMITER;
import static org.openbase.bco.manager.device.binding.openhab.util.configgen.items.AbstractItemEntry.ITEM_SUBSEGMENT_DELIMITER;
import static org.openbase.bco.manager.device.binding.openhab.util.configgen.items.LocationItemEntry.LOCATION_RSB_BINDING_CONFIG;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.processing.StringProcessor;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ConnectionItemEntry extends AbstractItemEntry {

    public static String CONNECTION_GROUP_LABEL = "Connections";

    public ConnectionItemEntry(final UnitConfig connectionUnitConfig, final ServiceTemplate serviceTemplate) throws org.openbase.jul.exception.InstantiationException {
        super(connectionUnitConfig, null);
        try {
            this.itemId = generateItemId(connectionUnitConfig, serviceTemplate.getType());
            this.icon = "";
            this.commandType = getDefaultCommand(serviceTemplate);
            this.label = connectionUnitConfig.getLabel();
            this.itemHardwareConfig = "rsb=\"" + LOCATION_RSB_BINDING_CONFIG + ":" + connectionUnitConfig.getId() + "\"";
            groups.add(CONNECTION_GROUP_LABEL);
            groups.add(GroupEntry.generateGroupID(connectionUnitConfig.getScope()));
            calculateGaps();
        } catch (CouldNotPerformException ex) {
            throw new org.openbase.jul.exception.InstantiationException(this, ex);
        }
    }

    private String generateItemId(final UnitConfig connectionUnitConfig, ServiceType serviceType) throws CouldNotPerformException {
        return StringProcessor.transformToIdString("Connection")
                + ITEM_SEGMENT_DELIMITER
                + StringProcessor.transformUpperCaseToCamelCase(serviceType.name())
                + ITEM_SEGMENT_DELIMITER
                + ScopeGenerator.generateStringRepWithDelimiter(connectionUnitConfig.getScope(), ITEM_SUBSEGMENT_DELIMITER);
    }
}
