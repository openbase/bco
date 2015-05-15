package de.citec.dal.util;

import de.citec.dal.registry.DeviceRegistry;
import de.citec.jul.exception.CouldNotPerformException;

/**
 *
 * @author thuxohl
 */
public interface DeviceInitializer {

    public void initDevices(final DeviceRegistry registry) throws CouldNotPerformException, InterruptedException;
}
