/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab.transform;

import de.citec.dal.hal.device.Device;
import de.citec.dal.hal.service.Service;
import de.citec.dal.hal.unit.Unit;
import de.citec.jul.exception.CouldNotPerformException;

/**
 *
 * @author Divine Threepwood
 */
public class ItemTransformer {

    public static final String ITEM_SUBSEGMENT_DELIMITER = "_";
    public static final String ITEM_SEGMENT_DELIMITER = "__";

    public static String generateItemName(final Device device, final Unit unit, final Service service) throws CouldNotPerformException {
        return service.getServiceConfig().getBindingServiceConfig().getOpenhabBindingServiceConfig().getItemId();
//        
//                + ITEM_SEGMENT_DELIMITER
//                + generateLocationId(unit.getLocation())
//                + ITEM_SEGMENT_DELIMITER
//                + StringProcessor.transformUpperCaseToCamelCase(unit.getType().name())
//                + ITEM_SEGMENT_DELIMITER
//                + unit.getLabel()
//                + ITEM_SEGMENT_DELIMITER
//                + service.getServiceType().getServiceName();
    }

//    public static String generateLocationId(final Location location) throws CouldNotPerformException {
//        String location_id = "";
//
//        boolean firstEntry = true;
//        for (String component : location.getScope().getComponents()) {
//            if (firstEntry) {
//                firstEntry = false;
//            } else {
//                location_id += ITEM_SUBSEGMENT_DELIMITER;
//            }
//            location_id += component;
//
//        }
//        return location_id;
//    }

//    public static String generateUnitID(final OpenhabCommandType.OpenhabCommand command) throws CouldNotPerformException {
//        return generateUnitID(command.getItem());
//    }
//
//    public static String generateUnitID(final String itemName) throws CouldNotPerformException {
//        try {
//            String[] nameSegment = itemName.split(ITEM_SEGMENT_DELIMITER);
//            String location = nameSegment[1].replace(ITEM_SUBSEGMENT_DELIMITER, Scope.COMPONENT_SEPARATOR);
//            return location + Scope.COMPONENT_SEPARATOR + nameSegment[2] + Scope.COMPONENT_SEPARATOR + nameSegment[3];
//        } catch (Exception ex) {
//            throw new CouldNotPerformException("Could not generate unit id! Item not compatible!");
//        }
//    }
}
