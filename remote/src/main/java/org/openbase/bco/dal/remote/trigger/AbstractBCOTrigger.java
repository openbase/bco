package org.openbase.bco.dal.remote.trigger;

/*-
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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

import com.google.protobuf.Message;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.remote.layer.unit.AbstractUnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.controller.Remote;
import org.openbase.type.domotic.state.ConnectionStateType.ConnectionState;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.jul.pattern.trigger.AbstractTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState;

import java.lang.reflect.Method;

/**
 * @param <UR>  UnitRemote
 * @param <DT>  DataType
 * @param <STE> StateTypeEnum
 * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 */
public abstract class AbstractBCOTrigger<UR extends AbstractUnitRemote<DT>, DT extends Message, STE> extends AbstractTrigger {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    private final UR unitRemote;
    private final STE targetState;
    private final ServiceType serviceType;
    private final Observer<DataProvider<DT>, DT> dataObserver;
    private final Observer<Remote<?>, ConnectionState.State> connectionObserver;
    private boolean active = false;

    public AbstractBCOTrigger(final UR unitRemote, final STE targetState, final ServiceType serviceType) throws InstantiationException {
        super();
        this.unitRemote = unitRemote;
        this.targetState = targetState;
        this.serviceType = serviceType;

        this.dataObserver = (DataProvider<DT> source, DT data) -> {
            verifyCondition(data, targetState, serviceType);
        };

        this.connectionObserver = (Remote<?> source, ConnectionState.State data) -> {
            if (data.equals(ConnectionState.State.CONNECTED)) {
                verifyCondition(unitRemote.getData(), targetState, serviceType);
            } else {
                notifyChange(TimestampProcessor.updateTimestampWithCurrentTime(ActivationState.newBuilder().setValue(ActivationState.State.UNKNOWN).build()));
            }
        };
    }

    protected abstract void verifyCondition(final DT data, final STE targetState, final ServiceType serviceType);

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        unitRemote.addDataObserver(dataObserver);
        unitRemote.addConnectionStateObserver(connectionObserver);
        active = true;
        if (unitRemote.isDataAvailable()) {
            verifyCondition(unitRemote.getData(), targetState, serviceType);
        }
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        unitRemote.removeDataObserver(dataObserver);
        unitRemote.removeConnectionStateObserver(connectionObserver);
        active = false;
        notifyChange(TimestampProcessor.updateTimestampWithCurrentTime(ActivationState.newBuilder().setValue(ActivationState.State.UNKNOWN).build()));
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void shutdown() {
        try {
            deactivate();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not shutdown " + this, ex, LOGGER);
        }
        super.shutdown();
    }
}
