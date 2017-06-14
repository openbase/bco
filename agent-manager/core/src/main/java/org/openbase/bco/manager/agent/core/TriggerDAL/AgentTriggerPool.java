package org.openbase.bco.manager.agent.core.TriggerDAL;

/*-
 * #%L
 * BCO Manager Agent Core
 * %%
 * Copyright (C) 2015 - 2017 openbase.org
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
import org.openbase.bco.manager.agent.core.TriggerJUL.AbstractTrigger;
import java.util.ArrayList;
import java.util.List;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import org.openbase.jul.iface.Activatable;
import org.openbase.jul.iface.Shutdownable;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.ObservableImpl;
import org.openbase.jul.pattern.Observer;
import org.slf4j.LoggerFactory;
import rst.domotic.state.ActivationStateType.ActivationState;

/**
 *
 * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 */
public class AgentTriggerPool implements Activatable, Shutdownable {

    public static enum TriggerOperation {
        AND, OR
    }

    private boolean active;

    private final ObservableImpl<ActivationState> triggerObservable;

    private final List<AbstractTrigger> triggerListAND;
    private final List<AbstractTrigger> triggerListOR;
    private final Observer<ActivationState> triggerAndObserver;
    private final Observer<ActivationState> triggerOrObserver;

    public AgentTriggerPool() throws InstantiationException {
        triggerObservable = new ObservableImpl<>(this);
        triggerListAND = new ArrayList();
        triggerListOR = new ArrayList();
        active = false;
        triggerAndObserver = (Observable<ActivationState> source, ActivationState data) -> {
            verifyCondition();
        };
        triggerOrObserver = (Observable<ActivationState> source, ActivationState data) -> {
            if (data.getValue().equals(ActivationState.State.ACTIVE)) {
                if (!triggerObservable.getValue().getValue().equals(ActivationState.State.ACTIVE)) {
                    triggerObservable.notifyObservers(TimestampProcessor.updateTimestampWithCurrentTime(ActivationState.newBuilder().setValue(ActivationState.State.ACTIVE).build()));
                }
            } else {
                verifyCondition();
            }
        };

        try {
            triggerObservable.notifyObservers(TimestampProcessor.updateTimestampWithCurrentTime(ActivationState.newBuilder().setValue(ActivationState.State.UNKNOWN).build()));
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException("Could not set initial state", ex);
        }
    }

    public void addTrigger(AbstractTrigger trigger, TriggerOperation triggerOperation) throws CouldNotPerformException {
        if (triggerOperation == TriggerOperation.AND) {
            triggerListAND.add(trigger);
        } else {
            triggerListOR.add(trigger);
        }
        if (active) {
            if (triggerOperation == TriggerOperation.AND) {
                trigger.registerObserver(triggerAndObserver);
            } else {
                trigger.registerObserver(triggerOrObserver);
            }
            try {
                trigger.activate();
            } catch (InterruptedException ex) {
                throw new CouldNotPerformException("Could not activate Trigger.", ex);
            }

            try {
                verifyCondition();
            } catch (NotAvailableException ex) {
                //ExceptionPrinter.printHistory("Data not available " + trigger, ex, LoggerFactory.getLogger(getClass()));
            }
        }
    }

    public void removeTrigger(AbstractTrigger trigger) {
        if (triggerListAND.contains(trigger)) {
            trigger.deregisterObserver(triggerAndObserver);
            triggerListAND.remove(trigger);
        } else if (triggerListOR.contains(trigger)) {
            trigger.deregisterObserver(triggerOrObserver);
            triggerListOR.remove(trigger);
        }
    }

    public void registerObserver(Observer<ActivationState> observer) {
        triggerObservable.addObserver(observer);
    }

    public void deregisterObserver(Observer<ActivationState> observer) {
        triggerObservable.removeObserver(observer);
    }

    public ActivationState getActivationState() throws NotAvailableException {
        return triggerObservable.getValue();
    }

    private void verifyCondition() throws CouldNotPerformException {
        if (verifyOrCondition() || verifyAndCondition()) {
            if (!triggerObservable.getValue().getValue().equals(ActivationState.State.ACTIVE)) {
                triggerObservable.notifyObservers(TimestampProcessor.updateTimestampWithCurrentTime(ActivationState.newBuilder().setValue(ActivationState.State.ACTIVE).build()));
            }
        } else {
            if (!triggerObservable.getValue().getValue().equals(ActivationState.State.DEACTIVE)) {
                triggerObservable.notifyObservers(TimestampProcessor.updateTimestampWithCurrentTime(ActivationState.newBuilder().setValue(ActivationState.State.DEACTIVE).build()));
            }
        }
    }

    private boolean verifyAndCondition() throws CouldNotPerformException {
        for (AbstractTrigger abstractTrigger : triggerListAND) {
            if (!abstractTrigger.getActivationState().getValue().equals(ActivationState.State.ACTIVE) && !abstractTrigger.getActivationState().getValue().equals(ActivationState.State.UNKNOWN)) {
                return false;
            }
        }
        return !triggerListAND.isEmpty();
    }

    private boolean verifyOrCondition() throws CouldNotPerformException {
        for (AbstractTrigger abstractTrigger : triggerListOR) {
            if (abstractTrigger.getActivationState().getValue().equals(ActivationState.State.ACTIVE)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        for (AbstractTrigger abstractTrigger : triggerListAND) {
            abstractTrigger.registerObserver(triggerAndObserver);
            abstractTrigger.activate();
        }
        for (AbstractTrigger abstractTrigger : triggerListOR) {
            abstractTrigger.registerObserver(triggerOrObserver);
            abstractTrigger.activate();
        }
        verifyCondition();
        active = true;
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        for (AbstractTrigger abstractTrigger : triggerListAND) {
            abstractTrigger.deregisterObserver(triggerAndObserver);
            abstractTrigger.deactivate();
        }
        for (AbstractTrigger abstractTrigger : triggerListOR) {
            abstractTrigger.deregisterObserver(triggerOrObserver);
            abstractTrigger.deactivate();
        }
        triggerObservable.notifyObservers(TimestampProcessor.updateTimestampWithCurrentTime(ActivationState.newBuilder().setValue(ActivationState.State.UNKNOWN).build()));
        active = false;
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
        triggerObservable.shutdown();
    }
}
