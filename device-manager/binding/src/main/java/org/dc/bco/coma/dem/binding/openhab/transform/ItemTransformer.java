/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.coma.dem.binding.openhab.transform;

import org.dc.bco.dal.lib.layer.service.Service;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.rst.processing.MetaConfigVariableProvider;

/**
 *
 * @author Divine Threepwood
 */
public class ItemTransformer {

    public static final String OPENHAB_BINDING_ITEM_ID = "OPENHAB_BINDING_ITEM_ID";

    public static String getItemName(final Service service) throws CouldNotPerformException {
        try {
            if (!service.getServiceConfig().hasBindingServiceConfig()) {
                throw new NotAvailableException("binding service config");
            }

            if (!service.getServiceConfig().getBindingServiceConfig().hasMetaConfig()) {
                throw new NotAvailableException("binding service config meta config");
            }

            MetaConfigVariableProvider metaConfigVariableProvider = new MetaConfigVariableProvider("BindingServiceConfig", service.getServiceConfig().getBindingServiceConfig().getMetaConfig());
            
            
            return metaConfigVariableProvider.getValue(OPENHAB_BINDING_ITEM_ID);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not generate item name!", ex);
        }
    }
}
