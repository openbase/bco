package org.dc.bco.manager.location.core;

/*
 * #%L
 * COMA LocationManager Core
 * %%
 * Copyright (C) 2015 - 2016 DivineCooperation
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import org.dc.bco.dal.lib.layer.service.ServiceFactory;
import org.dc.bco.dal.lib.layer.unit.AbstractUnitCollectionController;
import org.dc.bco.manager.location.lib.Location;
import org.dc.bco.registry.device.lib.DeviceRegistry;
import org.dc.bco.registry.location.lib.LocationRegistry;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.NotAvailableException;
import rst.spatial.LocationConfigType.LocationConfig;
import rst.spatial.LocationDataType.LocationData;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public class LocationController extends AbstractUnitCollectionController<LocationData, LocationData.Builder, LocationConfig> implements Location {

    public LocationController(final LocationConfig config) throws InstantiationException {
        super(LocationData.newBuilder());
    }

    @Override
    public ServiceFactory getServiceFactory() throws NotAvailableException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public LocationRegistry getLocationRegistry() throws NotAvailableException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public DeviceRegistry getDeviceRegistry() throws NotAvailableException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getLabel() throws NotAvailableException {
        return getConfig().getLabel();
    }
}
