package org.openbase.bco.dal.control.layer.unit.gateway;

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
import org.openbase.bco.dal.control.layer.unit.gateway.AbstractGatewayController;
import org.openbase.bco.dal.lib.layer.service.OperationServiceFactory;
import org.openbase.bco.dal.lib.layer.service.UnitDataSourceFactory;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 * * @author <a href="mailto:mpohling@techfak.uni-bielefeld.com">Marian Pohling</a>
 */
public class GenericGatewayController extends AbstractGatewayController {

    private final OperationServiceFactory operationServiceFactory;
    private final UnitDataSourceFactory unitDataSourceFactory;

    public GenericGatewayController(final OperationServiceFactory operationServiceFactory, final UnitDataSourceFactory unitDataSourceFactory) throws CouldNotPerformException {
        try {
            if (operationServiceFactory == null) {
                throw new NotAvailableException("service factory");
            }
            this.operationServiceFactory = operationServiceFactory;
            this.unitDataSourceFactory = unitDataSourceFactory;
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    public OperationServiceFactory getOperationServiceFactory() {
        return operationServiceFactory;
    }

    @Override
    public UnitDataSourceFactory getUnitDataSourceFactory() throws NotAvailableException {
        if (unitDataSourceFactory == null) {
            throw new NotAvailableException(UnitDataSourceFactory.class);
        }
        return unitDataSourceFactory;
    }
}
