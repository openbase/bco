/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab.transform;

import de.citec.dal.bindings.openhab.util.configgen.ItemEntry;
import de.citec.dal.hal.device.Device;
import de.citec.dal.hal.service.Service;
import de.citec.dal.hal.unit.Unit;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.NotAvailableException;

/**
 *
 * @author Divine Threepwood
 */
public class ItemTransformer {

    public static final String ITEM_SUBSEGMENT_DELIMITER = "_";
    public static final String ITEM_SEGMENT_DELIMITER = "__";
    public static final String OPENHAB_BINDING_ITEM_ID = "OPENHAB_BINDING_ITEM_ID";

    public static String generateItemName(final Device device, final Unit unit, final Service service) throws CouldNotPerformException {
        try {
            if (!service.getServiceConfig().hasBindingServiceConfig()) {
                throw new NotAvailableException("binding service config");
            }

            if (!service.getServiceConfig().getBindingServiceConfig().hasMetaConfig()) {
                throw new NotAvailableException("binding service config meta config");
            }

            ItemEntry.MetaConfigVariableProvider metaConfigVariableProvider = new ItemEntry.MetaConfigVariableProvider("BindingServiceConfig", service.getServiceConfig().getBindingServiceConfig().getMetaConfig());
            
            
            return metaConfigVariableProvider.getValue(OPENHAB_BINDING_ITEM_ID);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not generate item name!", ex);
        }
    }
}
