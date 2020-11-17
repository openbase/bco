package org.openbase.bco.dal.control.layer.unit.device;

/*
 * #%L
 * BCO DAL Control
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
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

import org.openbase.bco.dal.control.layer.unit.AbstractHostUnitController;
import org.openbase.bco.dal.lib.layer.unit.UnitController;
import org.openbase.bco.dal.lib.layer.unit.device.DeviceController;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.schedule.CloseableWriteLockWrapper;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.device.DeviceDataType.DeviceData;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public abstract class AbstractDeviceController extends AbstractHostUnitController<DeviceData, DeviceData.Builder, UnitController<?, ?>> implements DeviceController {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(DeviceData.getDefaultInstance()));
    }

    public AbstractDeviceController() throws InstantiationException {
        super(DeviceData.newBuilder());
    }

    @Override
    public UnitConfig applyConfigUpdate(final UnitConfig config) throws CouldNotPerformException, InterruptedException {
        try (final CloseableWriteLockWrapper ignored = getManageWriteLockInterruptible(this)) {
            final UnitConfig unitConfig = super.applyConfigUpdate(config);

            // update unit controller registry if device is active.
            for (final String removedUnitId : getRemovedUnitIds()) {
                DeviceManagerImpl.getDeviceManager().getUnitControllerRegistry().remove(removedUnitId);
            }

            for (final UnitController newUnitController : getNewUnitController()) {
                DeviceManagerImpl.getDeviceManager().getUnitControllerRegistry().register(newUnitController);
            }

            return unitConfig;
        }
    }

    @Override
    public List<UnitConfig> getHostedUnitConfigList() throws NotAvailableException, InterruptedException {
        List<UnitConfig> unitConfigs = new ArrayList<>();
        try {
            for (final String unitId : getConfig().getDeviceConfig().getUnitIdList()) {
                final UnitConfig unitConfig = Registries.getUnitRegistry(true).getUnitConfigById(unitId);
                if (UnitConfigProcessor.isEnabled(unitConfig)) {
                    unitConfigs.add(unitConfig);
                }
            }
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Hosted units description of Device", this, ex);
        }
        return unitConfigs;
    }

    @Override
    public void shutdown() {
        try {
            // skip if registry is shutting down anyway.
            if (!DeviceManagerImpl.getDeviceManager().getUnitControllerRegistry().isShutdownInitiated()) {
                DeviceManagerImpl.getDeviceManager().getUnitControllerRegistry().removeAllByKey(getHostedUnitControllerIdList());
            }
        } catch (InvalidStateException ex) {
            // registry already shutting down during removal which will clear the entries anyway.
        } catch (MultiException ex) {
            ExceptionPrinter.printHistory("Could not deregister all unit controller during shutdown!", ex, logger, LogLevel.WARN);
        }
        super.shutdown();
    }
}
