package org.openbase.bco.dal.control.layer.unit.app;

/*
 * #%L
 * BCO DAL Control
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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
import org.openbase.bco.dal.lib.layer.service.OperationServiceFactory;
import org.openbase.bco.dal.control.layer.unit.AbstractExecutableBaseUnitController;
import org.openbase.bco.dal.lib.layer.unit.UnitController;
import org.openbase.bco.dal.lib.layer.unit.app.AppController;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.NotSupportedException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.state.ActivationStateType.ActivationState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.app.AppDataType.AppData;

import java.util.Collections;
import java.util.List;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public abstract class AbstractAppController extends AbstractExecutableBaseUnitController<AppData, AppData.Builder> implements AppController {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AppData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActivationState.getDefaultInstance()));
    }

    private final OperationServiceFactory operationServiceFactory;

    public AbstractAppController(final Class unitClass) throws org.openbase.jul.exception.InstantiationException {
        this(unitClass, null);
    }

    public AbstractAppController(final Class unitClass, final OperationServiceFactory operationServiceFactory) throws org.openbase.jul.exception.InstantiationException {
        super(unitClass, AppData.newBuilder());
        this.operationServiceFactory = operationServiceFactory;
    }

    @Override
    public OperationServiceFactory getOperationServiceFactory() throws NotAvailableException {
        if (operationServiceFactory == null) {
            throw new NotAvailableException("ServiceFactory", new NotSupportedException("Unit hosting", this));
        }
        return operationServiceFactory;
    }

    @Override
    protected boolean isAutostartEnabled() throws CouldNotPerformException {
        return getConfig().getAppConfig().getAutostart();
    }

    @Override
    public List<UnitController<?, ?>> getHostedUnitControllerList() {
        // Method can be overwritten in case this app introduces further units.
        return Collections.EMPTY_LIST;
    }

    @Override
    public UnitController<?, ?> getHostedUnitController(String id) throws NotAvailableException {
        // Method can be overwritten in case this app introduces further units.
        throw new NotAvailableException("UnitController", id);
    }

    @Override
    public List<UnitConfig> getHostedUnitConfigList() {
        // Method can be overwritten in case this app introduces further units.
        return Collections.EMPTY_LIST;
    }
}
