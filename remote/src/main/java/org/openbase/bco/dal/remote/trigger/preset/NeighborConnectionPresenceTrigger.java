package org.openbase.bco.dal.remote.trigger.preset;

/*-
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
import org.openbase.bco.dal.remote.unit.connection.ConnectionRemote;
import org.openbase.bco.dal.remote.unit.location.LocationRemote;
import org.openbase.jul.pattern.trigger.AbstractTrigger;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.Remote;
import org.slf4j.LoggerFactory;
import rst.domotic.state.ActivationStateType.ActivationState;
import rst.domotic.state.DoorStateType.DoorState;
import rst.domotic.state.PresenceStateType.PresenceState;
import rst.domotic.state.WindowStateType.WindowState;

/**
 *
 * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 */
public class NeighborConnectionPresenceTrigger extends AbstractTrigger {

    private final Observer dataObserver;
    private final Observer<Remote.ConnectionState> connectionObserver;
    private final LocationRemote locationRemote;
    private final ConnectionRemote connectionRemote;
    private boolean active = false;

    public NeighborConnectionPresenceTrigger(final LocationRemote locationRemote, final ConnectionRemote connectionRemote) throws org.openbase.jul.exception.InstantiationException {
        super();

        this.locationRemote = locationRemote;
        this.connectionRemote = connectionRemote;

        dataObserver = (source, data) -> {
            verifyCondition();
        };

        connectionObserver = (Observable<Remote.ConnectionState> source, Remote.ConnectionState data) -> {
            if (data.equals(Remote.ConnectionState.CONNECTED)) {
                verifyCondition();
            } else {
                notifyChange(TimestampProcessor.updateTimestampWithCurrentTime(ActivationState.newBuilder().setValue(ActivationState.State.UNKNOWN).build()));
            }
        };
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        locationRemote.addDataObserver(dataObserver);
        connectionRemote.addDataObserver(dataObserver);
        locationRemote.addConnectionStateObserver(connectionObserver);
        connectionRemote.addConnectionStateObserver(connectionObserver);
        active = true;
        verifyCondition();
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        locationRemote.removeDataObserver(dataObserver);
        connectionRemote.removeDataObserver(dataObserver);
        locationRemote.removeConnectionStateObserver(connectionObserver);
        connectionRemote.removeConnectionStateObserver(connectionObserver);
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
            ExceptionPrinter.printHistory("Could not shutdown " + this, ex, LoggerFactory.getLogger(getClass()));
        }
        super.shutdown();
    }

    private void verifyCondition() {
        try {
            if (locationRemote.getData().getPresenceState().getValue().equals(PresenceState.State.PRESENT)
                    && (connectionRemote.getDoorState().getValue().equals(DoorState.State.OPEN)
                    || connectionRemote.getWindowState().getValue().equals(WindowState.State.OPEN))) {
                notifyChange(TimestampProcessor.updateTimestampWithCurrentTime(ActivationState.newBuilder().setValue(ActivationState.State.ACTIVE).build()));
            } else {
                notifyChange(TimestampProcessor.updateTimestampWithCurrentTime(ActivationState.newBuilder().setValue(ActivationState.State.DEACTIVE).build()));
            }
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not verify trigger state " + this, ex, LoggerFactory.getLogger(getClass()));
        }
    }
}
