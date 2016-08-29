package org.openbase.bco.manager.location.remote;

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
import org.openbase.bco.manager.location.lib.Connection;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rsb.com.AbstractConfigurableRemote;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.HandleStateType.HandleState;
import rst.homeautomation.state.ContactStateType.ContactState;
import rst.spatial.ConnectionConfigType.ConnectionConfig;
import rst.spatial.ConnectionDataType.ConnectionData;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class ConnectionRemote extends AbstractConfigurableRemote<ConnectionData, ConnectionConfig> implements Connection {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ConnectionData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(HandleState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ContactState.getDefaultInstance()));
    }

    public ConnectionRemote() {
        super(ConnectionData.class, ConnectionConfig.class);
    }

    @Override
    public void notifyDataUpdate(ConnectionData data) throws CouldNotPerformException {
    }

    @Override
    public String getLabel() throws NotAvailableException {
        try {
            if (config == null) {
                throw new NotAvailableException("connectionConfig");
            }
            return config.getLabel();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("label", ex);
        }
    }

    @Override
    public HandleState getHandleState() throws NotAvailableException {
        // TODO: connection has DOOR/WINDOW or PASSAGE state
        throw new UnsupportedOperationException();
    }

    @Override
    public ContactState getContactState() throws NotAvailableException {
        // TODO: connection has DOOR/WINDOW or PASSAGE state
        throw new UnsupportedOperationException();
    }

    @Override
    public ConnectionConfig applyConfigUpdate(ConnectionConfig config) throws CouldNotPerformException, InterruptedException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
