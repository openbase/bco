package org.openbase.bco.dal.remote.unit.connection;

/*
 * #%L
 * COMA LocationManager Remote
 * %%
 * Copyright (C) 2015 - 2016 openbase.org
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
import org.openbase.bco.dal.lib.layer.unit.connection.Connection;
import org.openbase.bco.dal.remote.unit.AbstractUnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.state.ContactStateType.ContactState;
import rst.domotic.state.DoorStateType.DoorState;
import rst.domotic.state.HandleStateType.HandleState;
import rst.domotic.state.PassageStateType.PassageState;
import rst.domotic.state.WindowStateType.WindowState;
import rst.domotic.unit.connection.ConnectionDataType.ConnectionData;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ConnectionRemote extends AbstractUnitRemote<ConnectionData> implements Connection {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ConnectionData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(HandleState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ContactState.getDefaultInstance()));
    }

    public ConnectionRemote() {
        super(ConnectionData.class);
    }

    @Override
    public void notifyDataUpdate(final ConnectionData data) throws CouldNotPerformException {
    }

    @Override
    public String getLabel() throws NotAvailableException {
        return getConfig().getLabel();
    }

    @Override
    public DoorState getDoorState() throws NotAvailableException {
        try {
            return getData().getDoorState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("DoorState", ex);
        }
    }

    @Override
    public PassageState getPassageState() throws NotAvailableException {
        try {
            return getData().getPassageState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("PassageState", ex);
        }
    }

    @Override
    public WindowState getWindowState() throws NotAvailableException {
        try {
            return getData().getWindowState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("WindowState", ex);
        }
    }
}
