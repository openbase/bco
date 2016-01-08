package org.dc.bco.manager.device.binding.openhab;

import org.dc.bco.manager.device.binding.openhab.comm.OpenHABCommunicator;
import org.dc.jul.exception.NotAvailableException;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public interface OpenHABBinding {
    public OpenHABCommunicator getBusCommunicator() throws NotAvailableException;
}
