package org.dc.bco.manager.location.core;

import org.dc.bco.manager.location.lib.Location;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.pattern.Factory;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public interface LocationFactory extends Factory<Location, LocationConfig> {

    @Override
    public LocationController newInstance(final LocationConfig config) throws InstantiationException;

}
