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
    }
}
