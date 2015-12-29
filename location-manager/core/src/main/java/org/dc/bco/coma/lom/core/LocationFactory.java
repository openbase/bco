package org.dc.bco.coma.lom.core;

import org.dc.bco.coma.lom.lib.Location;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.pattern.Factory;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public interface LocationFactory extends Factory<Location, LocationConfig> {

    @Override
    public LocationController newInstance(final LocationConfig config) throws CouldNotPerformException;

}
