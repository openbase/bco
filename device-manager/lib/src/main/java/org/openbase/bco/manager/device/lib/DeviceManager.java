package org.openbase.bco.manager.device.lib;

/*
 * #%L
 * BCO Manager Device Library
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
import org.openbase.bco.dal.lib.layer.unit.UnitControllerRegistry;
import org.openbase.bco.dal.lib.layer.service.ServiceFactoryProvider;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.storage.registry.RegistryImpl;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface DeviceManager extends ServiceFactoryProvider, DeviceFactoryProvider {

    public RegistryImpl<String, DeviceController> getDeviceControllerRegistry() throws NotAvailableException;

    public UnitControllerRegistry getUnitControllerRegistry() throws NotAvailableException;

    public boolean isSupported(final UnitConfig config) throws CouldNotPerformException;
}
