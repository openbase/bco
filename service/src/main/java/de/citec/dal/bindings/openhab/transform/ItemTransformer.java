/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab.transform;

import de.citec.dal.data.Location;
import de.citec.dal.hal.device.Device;
import de.citec.dal.hal.service.Service;
import de.citec.dal.hal.unit.Unit;
import de.citec.jul.exception.CouldNotPerformException;
import rsb.Scope;
import rst.homeautomation.openhab.OpenhabCommandType;

/**
 *
 * @author Divine Threepwood
 */
public class ItemTransformer {

	public static final String ITEM_SUBSEGMENT_DELIMITER = "_";
	public static final String ITEM_SEGMENT_DELIMITER = "__";

	public static String generateItemName(final Device device, final Unit unit, final Service service) throws CouldNotPerformException {
		return device.getName()
				+ ITEM_SEGMENT_DELIMITER
				+ generateLocationId(unit.getLocation())
				+ ITEM_SEGMENT_DELIMITER
				+ unit.getName()
				+ ITEM_SEGMENT_DELIMITER
				+ unit.getLabel()
				+ ITEM_SEGMENT_DELIMITER
				+ service.getServiceType().getServiceName();
	}

	public static String generateLocationId(final Location location) throws CouldNotPerformException {
		String location_id = "";

        for(String component : location.getScope().getComponents()) {
            location_id += component + ITEM_SUBSEGMENT_DELIMITER;
        }
        return location_id;
	}

	public static String generateUnitID(final OpenhabCommandType.OpenhabCommand command) {
		return generateUnitID(command.getItem());
	}

	public static String generateUnitID(final String itemName) {
		String[] nameSegment = itemName.split(ITEM_SEGMENT_DELIMITER);
		String location = nameSegment[1].replace(ITEM_SUBSEGMENT_DELIMITER, Scope.COMPONENT_SEPARATOR);
		return location + Scope.COMPONENT_SEPARATOR + nameSegment[2] + Scope.COMPONENT_SEPARATOR + nameSegment[3];
	}
}
