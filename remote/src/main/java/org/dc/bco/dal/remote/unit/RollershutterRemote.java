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
import org.dc.bco.dal.lib.layer.unit.RollershutterInterface;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.rsb.com.RPCHelper;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.ShutterStateType.ShutterState;
import rst.homeautomation.unit.RollershutterType.Rollershutter;

/**
 *
 * @author thuxohl
 */
public class RollershutterRemote extends AbstractUnitRemote<Rollershutter> implements RollershutterInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(Rollershutter.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ShutterState.getDefaultInstance()));
    }

    public RollershutterRemote() {
    }

    @Override
    public void notifyDataUpdate(Rollershutter data) {
    }

    public void setShutter(ShutterState.State value) throws CouldNotPerformException {
        setShutter(ShutterState.newBuilder().setValue(value).build());
    }

    @Override
    public Future<Void> setShutter(ShutterState value) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(value, this, Void.class);
    }

    @Override
    public ShutterState getShutter() throws NotAvailableException {
        try {
            return getData().getShutterState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("ShutterState", ex);
        }
    }

    @Override
    public Future<Void> setOpeningRatio(final Double value) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(value, this, Void.class);
    }

    @Override
    public Double getOpeningRatio() throws NotAvailableException {
        try {
            return getData().getOpeningRatio();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("OpeningRatio", ex);
        }
    }

}
