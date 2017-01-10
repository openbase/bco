package org.openbase.bco.dal.remote.unit;

/*
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.state.BlindStateType.BlindState;
import rst.domotic.unit.dal.RollerShutterDataType.RollerShutterData;
import org.openbase.bco.dal.lib.layer.unit.RollerShutter;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class RollerShutterRemote extends AbstractUnitRemote<RollerShutterData> implements RollerShutter {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(RollerShutterData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(BlindState.getDefaultInstance()));
    }

    public RollerShutterRemote() {
        super(RollerShutterData.class);
    }

    @Override
    public void notifyDataUpdate(RollerShutterData data) {
    }

    public Future<Void> setBlindState(BlindState.MovementState movementState) throws CouldNotPerformException {
        return RollerShutterRemote.this.setBlindState(BlindState.newBuilder().setMovementState(movementState).build());
    }

    @Override
    public Future<Void> setBlindState(BlindState value) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(value, this, Void.class);
    }

    @Override
    public BlindState getBlindState() throws NotAvailableException {
        try {
            return getData().getBlindState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("BlindState", ex);
        }
    }
}
