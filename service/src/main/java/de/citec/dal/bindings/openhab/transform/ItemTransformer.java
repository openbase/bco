/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab.transform;

import de.citec.dal.data.Location;
import de.citec.dal.hal.device.DeviceInterface;
import de.citec.dal.hal.service.Service;
import de.citec.dal.hal.unit.UnitInterface;
import de.citec.jul.exception.NotAvailableException;
import rsb.Scope;
import rst.homeautomation.openhab.OpenhabCommandType;

/**
 *
 * @author Divine <DivineThreepwood@gmail.com>
 */
public class ItemTransformer {

	public static final String ITEM_SUBSEGMENT_DELIMITER = "_";
	public static final String ITEM_SEGMENT_DELIMITER = "__";

	public static String generateItemName(final DeviceInterface device, final UnitInterface unit, final Service service) {
		return device.getName()
				+ ITEM_SEGMENT_DELIMITER
				+ generateLocationName(unit.getLocation())
				+ ITEM_SEGMENT_DELIMITER
				+ unit.getName()
				+ ITEM_SEGMENT_DELIMITER
				+ unit.getLabel()
				+ ITEM_SEGMENT_DELIMITER
				+ service.getServiceType().getServiceName();
	}

	public static String generateLocationName(final Location location) {
		String location_id = "";
		Location parentLocation;

		try {
			parentLocation = location.getParent();
			while (true) {
				location_id += parentLocation.getName() + ITEM_SUBSEGMENT_DELIMITER;
				parentLocation = parentLocation.getParent();
			}
		} catch (NotAvailableException ex) {
			return location_id + location.getName();
		}
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
