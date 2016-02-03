/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.device.binding.openhab.transform;

import org.dc.bco.dal.lib.layer.service.Service;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.rst.processing.MetaConfigVariableProvider;
import rst.homeautomation.service.ServiceConfigType;

/**
 *
 * @author Divine Threepwood
 */
public class ItemNameLoader {

    public static final String OPENHAB_BINDING_ITEM_ID = "OPENHAB_BINDING_ITEM_ID";

    public static String getItemName(final Service service, final ServiceConfigType.ServiceConfig serviceConfig) throws CouldNotPerformException {
        try {
            if (!serviceConfig.hasBindingServiceConfig()) {
                throw new NotAvailableException("binding service config");
            }

            if (!serviceConfig.getBindingServiceConfig().hasMetaConfig()) {
                throw new NotAvailableException("binding service config meta config");
            }

            MetaConfigVariableProvider metaConfigVariableProvider = new MetaConfigVariableProvider("BindingServiceConfig", serviceConfig.getBindingServiceConfig().getMetaConfig());
            
            
            return metaConfigVariableProvider.getValue(OPENHAB_BINDING_ITEM_ID);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not generate item name!", ex);
        }
    }
}
