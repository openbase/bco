package org.openbase.bco.dal.control.layer.unit.location;

/*
 * #%L
 * BCO DAL Control
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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
import org.openbase.bco.dal.lib.layer.unit.location.LocationController;
import org.openbase.bco.dal.lib.layer.unit.location.LocationControllerFactory;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 *
 */
public class LocationControllerFactoryImpl implements LocationControllerFactory {

    private static LocationControllerFactoryImpl instance;

    public synchronized static LocationControllerFactoryImpl getInstance() {
        if (instance == null) {
            instance = new LocationControllerFactoryImpl();
        }
        return instance;
    }

    @Override
    public LocationController newInstance(final UnitConfig config) throws InstantiationException, InterruptedException {
        LocationController locationController;
        try {
            if (config == null) {
                throw new NotAvailableException("locationconfig");
            }
            locationController = new LocationControllerImpl();
            locationController.init(config);
            return locationController;
        } catch (InstantiationException | NotAvailableException | InitializationException ex) {
            throw new InstantiationException(LocationControllerImpl.class, config.getId(), ex);
        }
    }
}
