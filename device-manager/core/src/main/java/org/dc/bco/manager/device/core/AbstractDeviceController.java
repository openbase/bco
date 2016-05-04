
package org.dc.bco.manager.device.core;

/*
 * #%L
 * COMA DeviceManager Core
 * %%
 * Copyright (C) 2015 - 2016 DivineCooperation
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

import org.dc.bco.dal.lib.layer.unit.AbstractUnitCollectionController;
import org.dc.bco.dal.lib.layer.unit.Unit;
import org.dc.bco.manager.device.lib.DeviceController;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.CouldNotTransformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.InvalidStateException;
import org.dc.jul.exception.NotAvailableException;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.device.GenericDeviceType.GenericDevice;

/**
 *
 * @author Divine Threepwood
 */
public abstract class AbstractDeviceController extends AbstractUnitCollectionController<GenericDevice, GenericDevice.Builder, DeviceConfig> implements DeviceController {

    public AbstractDeviceController() throws InstantiationException, CouldNotTransformException {
        super(GenericDevice.newBuilder());
//        try {
//        } catch (CouldNotPerformException ex) {
//            throw new InstantiationException(RSBCommunicationService.class, ex);
//        }
    }

    @Override
    public void init(final DeviceConfig config) throws InitializationException, InterruptedException {

        try {
            if (config == null) {
                throw new NotAvailableException("config");
            }

            if (!config.hasId()) {
                throw new NotAvailableException("config.id");
            }

            if (config.getId().isEmpty()) {
                throw new NotAvailableException("Field config.id is empty!");
            }

            if (!config.hasLabel()) {
                throw new NotAvailableException("config.label");
            }

            if (config.getLabel().isEmpty()) {
                throw new NotAvailableException("Field config.label is emty!");
            }

            super.init(config);

            try {
                registerUnits(config.getUnitConfigList());

                for (Unit unit : getUnits()) {
                    DeviceManagerController.getDeviceManager().getUnitControllerRegistry().register(unit);
                }
            } catch (CouldNotPerformException ex) {
                throw new InstantiationException(this, ex);
            }

        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

//    @Override
//    public DeviceConfig updateConfig(final DeviceConfig config) throws CouldNotPerformException {
////        setField(DEVICE_TYPE_FILED_CONFIG, config);
//        return super.updateConfig(config);
//    }

    @Override
    public final String getId() throws NotAvailableException {
        try {
            DeviceConfig tmpConfig = getConfig();
            if (!tmpConfig.hasId()) {
                throw new NotAvailableException("unitconfig.id");
            }

            if (tmpConfig.getId().isEmpty()) {
                throw new InvalidStateException("unitconfig.id is empty");
            }

            return tmpConfig.getId();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("id", ex);
        }
    }

    @Override
    public String getLabel() throws NotAvailableException {
        try {
            DeviceConfig tmpConfig = getConfig();
            if (!tmpConfig.hasId()) {
                throw new NotAvailableException("unitconfig.label");
            }

            if (tmpConfig.getId().isEmpty()) {
                throw new InvalidStateException("unitconfig.label is empty");
            }
            return getConfig().getLabel();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("label", ex);
        }
    }
}
