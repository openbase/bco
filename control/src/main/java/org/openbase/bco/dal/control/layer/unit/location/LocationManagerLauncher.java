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
import org.openbase.bco.dal.lib.layer.unit.location.LocationManager;
import org.openbase.bco.authentication.lib.BCO;
import org.openbase.jul.pattern.launch.AbstractLauncher;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 *
 */
public class LocationManagerLauncher extends AbstractLauncher<LocationManagerImpl> {

    public LocationManagerLauncher() throws org.openbase.jul.exception.InstantiationException {
        super(LocationManager.class, LocationManagerImpl.class);
    }

    @Override
    public void loadProperties() {
    }

    public static void main(String[] args) throws Throwable {
        BCO.printLogo();
        main(BCO.class, LocationManager.class, args, LocationManagerLauncher.class);
    }
}
