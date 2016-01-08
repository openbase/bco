package org.dc.bco.dal.lib.layer.service;

import org.dc.jul.exception.NotAvailableException;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public interface ServiceFactoryProvider {

    public ServiceFactory getServiceFactory() throws NotAvailableException;
}
