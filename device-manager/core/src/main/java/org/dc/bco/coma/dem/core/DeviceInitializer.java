package org.dc.bco.coma.dem.core;

import org.dc.bco.coma.dem.core.DeviceRegistry;
import org.dc.jul.exception.CouldNotPerformException;

/**
 *
 * @author thuxohl
 */
public interface DeviceInitializer {

    public void initDevices(final DeviceRegistry registry) throws CouldNotPerformException, InterruptedException;
}
