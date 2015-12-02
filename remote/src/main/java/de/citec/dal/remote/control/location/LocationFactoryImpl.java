package de.citec.dal.remote.control.location;

import de.citec.dal.remote.control.agent.AgentFactoryImpl;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.NotAvailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public class LocationFactoryImpl implements LocationFactory {

    protected final Logger logger = LoggerFactory.getLogger(AgentFactoryImpl.class);

    @Override
    public LocationController newInstance(final LocationConfig config) throws CouldNotPerformException {
        try {
            if (config == null) {
                throw new NotAvailableException("locationconfig");
            }
            return new LocationController(config);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not instantiate Location[" + config.getId() + "]!", ex);
        }
    }
}
