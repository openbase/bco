package de.citec.dal.util;

import de.citec.dal.registry.DeviceRegistry;

/**
 *
 * @author thuxohl
 */
public interface DeviceInitializer {

    public void initDevices(final DeviceRegistry registry);
}
