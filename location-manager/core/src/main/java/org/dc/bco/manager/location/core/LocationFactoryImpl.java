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

import org.dc.bco.manager.location.lib.LocationController;
import org.dc.bco.manager.location.lib.LocationFactory;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.exception.InstantiationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public class LocationFactoryImpl implements LocationFactory {

    protected final Logger logger = LoggerFactory.getLogger(LocationFactoryImpl.class);

    @Override
    public LocationController newInstance(final LocationConfig config) throws InstantiationException {
        try {
            if (config == null) {
                throw new NotAvailableException("locationconfig");
            }
            return new LocationControllerImpl(config);
        } catch (Exception ex) {
            throw new InstantiationException(LocationControllerImpl.class, config.getId(), ex);
        }
    }
}
