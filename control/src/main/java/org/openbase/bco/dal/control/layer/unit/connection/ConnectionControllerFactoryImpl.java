package org.openbase.bco.dal.control.layer.unit.connection;

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
import org.openbase.bco.dal.lib.layer.unit.connection.ConnectionController;
import org.openbase.bco.dal.lib.layer.unit.connection.ConnectionControllerFactory;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ConnectionControllerFactoryImpl implements ConnectionControllerFactory {

    private static ConnectionControllerFactoryImpl instance;

    public synchronized static ConnectionControllerFactoryImpl getInstance() {
        if (instance == null) {
            instance = new ConnectionControllerFactoryImpl();
        }
        return instance;
    }

    @Override
    public ConnectionController newInstance(final UnitConfig config) throws InstantiationException, InterruptedException {
        ConnectionController connectionController;
        try {
            if (config == null) {
                throw new NotAvailableException("connectionConfig");
            }
            connectionController = new ConnectionControllerImpl();
            connectionController.init(config);
            return connectionController;
        } catch (InstantiationException | NotAvailableException | InitializationException ex) {
            throw new InstantiationException(ConnectionControllerImpl.class, config.getId(), ex);
        }
    }
}
