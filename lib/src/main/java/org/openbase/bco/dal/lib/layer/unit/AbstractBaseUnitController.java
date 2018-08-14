package org.openbase.bco.dal.lib.layer.unit;

/*
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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

import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Message;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;

import java.util.concurrent.Future;

/**
 * @param <D>  the data type of this unit used for the state synchronization.
 * @param <DB> the builder used to build the unit data instance.
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public abstract class AbstractBaseUnitController<D extends GeneratedMessage, DB extends D.Builder<DB>> extends AbstractUnitController<D, DB> implements BaseUnitController<D, DB> {

    public AbstractBaseUnitController(final Class unitClass, final DB builder) throws InstantiationException {
        super(unitClass, builder);
    }

    @Override
    public Future<Void> performOperationService(final Message serviceState, final ServiceType serviceType) {
        return GlobalCachedExecutorService.submit(() -> {
            super.applyDataUpdate(TimestampProcessor.updateTimestampWithCurrentTime(serviceState), serviceType);
            return null;
        });
    }
}
