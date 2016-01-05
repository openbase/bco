package de.citec.dal.hal.service;

import org.dc.jul.exception.CouldNotPerformException;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public interface ServiceFactoryProvider {

    public ServiceFactory getServiceFactory() throws CouldNotPerformException;
}
