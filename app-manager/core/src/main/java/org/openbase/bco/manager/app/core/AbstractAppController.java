package org.openbase.bco.manager.app.core;

/*
 * #%L
 * BCO Manager App Core
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
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
import org.openbase.bco.dal.lib.layer.service.ServiceFactory;
import org.openbase.bco.dal.lib.layer.unit.AbstractExecutableBaseUnitController;
import org.openbase.bco.manager.app.lib.AppController;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.NotSupportedException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.state.ActivationStateType.ActivationState;
import rst.domotic.unit.app.AppDataType.AppData;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public abstract class AbstractAppController extends AbstractExecutableBaseUnitController<AppData, AppData.Builder> implements AppController {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AppData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActivationState.getDefaultInstance()));
    }

    private final ServiceFactory serviceFactory;

    public AbstractAppController(final Class unitClass) throws org.openbase.jul.exception.InstantiationException {
        this(unitClass, null);
    }

    public AbstractAppController(final Class unitClass, final ServiceFactory serviceFactory) throws org.openbase.jul.exception.InstantiationException {
        super(unitClass, AppData.newBuilder());
        this.serviceFactory = serviceFactory;
    }

    @Override
    public ServiceFactory getServiceFactory() throws NotAvailableException {
        if (serviceFactory == null) {
            throw new NotAvailableException("ServiceFactory", new NotSupportedException("Unit hosting", this));
        }
        return serviceFactory;
    }

    @Override
    protected boolean isAutostartEnabled() throws CouldNotPerformException {
        return getConfig().getAppConfig().getAutostart();
    }
}
