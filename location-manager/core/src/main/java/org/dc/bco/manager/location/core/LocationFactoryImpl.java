package org.dc.bco.manager.location.core;

import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.exception.InstantiationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public class LocationFactoryImpl implements LocationFactory {

    protected final Logger logger = LoggerFactory.getLogger(LocationFactoryImpl.class);

    @Override
    public LocationController newInstance(final LocationConfig config) throws InstantiationException {
        try {
            if (config == null) {
                throw new NotAvailableException("locationconfig");
            }
            return new LocationController(config);
        } catch (Exception ex) {
            throw new InstantiationException(LocationController.class, config.getId(), ex);
        }
    }
}
