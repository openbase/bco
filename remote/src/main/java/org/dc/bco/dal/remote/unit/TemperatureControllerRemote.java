package org.dc.bco.dal.remote.unit;

/*
 * #%L
 * DAL Remote
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
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
import java.util.concurrent.Future;
import org.dc.bco.dal.lib.layer.unit.TemperatureControllerInterface;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.rsb.com.RPCHelper;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.unit.TemperatureControllerType.TemperatureController;

/**
 *
 * @author mpohling
 */
public class TemperatureControllerRemote extends AbstractUnitRemote<TemperatureController> implements TemperatureControllerInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TemperatureController.getDefaultInstance()));
    }

    public TemperatureControllerRemote() {
    }

    @Override
    public void notifyDataUpdate(TemperatureController data) throws CouldNotPerformException {
    }

    @Override
    public Double getTemperature() throws NotAvailableException {
        try {
            return getData().getActualTemperature();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Temperature", ex);
        }
    }

    @Override
    public Future<Void> setTargetTemperature(Double value) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(value, this, Void.class);
    }

    @Override
    public Double getTargetTemperature() throws NotAvailableException {
        try {
            return getData().getTargetTemperature();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("TargetTemperature", ex);
        }
    }
}
