package org.openbase.bco.manager.device.core;

/*
 * #%L
 * BCO Manager Device Core
 * %%
 * Copyright (C) 2015 - 2017 openbase.org
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
import org.openbase.bco.dal.lib.layer.unit.AbstractHostUnitController;
import org.openbase.bco.dal.lib.layer.unit.UnitController;
import org.openbase.bco.dal.lib.layer.unit.UnitHost;
import org.openbase.bco.manager.device.lib.DeviceController;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.CouldNotTransformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.unit.device.DeviceDataType.DeviceData;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public abstract class AbstractDeviceController extends AbstractHostUnitController<DeviceData, DeviceData.Builder> implements DeviceController {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(DeviceData.getDefaultInstance()));
    }
    
    public AbstractDeviceController(final Class deviceClass) throws InstantiationException, CouldNotTransformException {
        super(deviceClass, DeviceData.newBuilder());
    }

    @Override
    public void init(final UnitConfig config) throws InitializationException, InterruptedException {
        try {
            super.init(config);
            try {
                registerUnitsById(config.getDeviceConfig().getUnitIdList());
                for (final UnitController unit : getHostedUnits()) {
                    DeviceManagerController.getDeviceManager().getUnitControllerRegistry().register(unit);
                }
            } catch (CouldNotPerformException ex) {
                throw new InstantiationException(this, ex);
            }

        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }
}
