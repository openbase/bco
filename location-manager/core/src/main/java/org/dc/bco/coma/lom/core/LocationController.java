package org.dc.bco.coma.lom.core;

import org.dc.bco.coma.lom.lib.Location;
import de.citec.dal.hal.device.AbstractUnitCollectionController;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.NotAvailableException;
import rst.homeautomation.device.GenericDeviceType.GenericDevice;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public class LocationController extends AbstractUnitCollectionController<GenericDevice, GenericDevice.Builder> implements Location {

    private LocationConfig config;

    public LocationController(final LocationConfig config) throws InstantiationException {
        super(GenericDevice.newBuilder());
        this.config = config;
    }

    @Override
    public String getId() throws CouldNotPerformException {
        return config.getId();
    }

    @Override
    public String getLabel() throws NotAvailableException {
        return config.getLabel();
    }
}
