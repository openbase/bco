package org.openbase.bco.dal.control.layer.unit;

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

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.Message;
import org.openbase.bco.dal.lib.layer.service.ServiceStateProcessor;
import org.openbase.bco.dal.lib.layer.unit.BaseUnitController;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;

import java.io.Serializable;
import java.util.concurrent.Future;

/**
 * @param <D>  the data type of this unit used for the state synchronization.
 * @param <DB> the builder used to build the unit data instance.
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public abstract class AbstractBaseUnitController<D extends AbstractMessage & Serializable, DB extends D.Builder<DB>> extends AbstractUnitController<D, DB> implements BaseUnitController<D, DB> {

    public AbstractBaseUnitController(final DB builder) throws InstantiationException {
        super(builder);
    }

    @Override
    public Future<ActionDescription> performOperationService(final Message serviceState, final ServiceType serviceType) {
        if (getOperationServiceMap().containsKey(serviceType)) {
            return super.performOperationService(serviceState, serviceType);
        }
        try {
            super.applyDataUpdate(TimestampProcessor.updateTimestampWithCurrentTime(serviceState), serviceType);
            return FutureProcessor.completedFuture(ServiceStateProcessor.getResponsibleAction(serviceState, () -> ActionDescription.getDefaultInstance()));
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, ex);
        }
    }
}
