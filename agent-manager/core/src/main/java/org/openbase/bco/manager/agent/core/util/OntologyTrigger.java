package org.openbase.bco.manager.agent.core.util;

import org.openbase.bco.ontology.lib.trigger.Trigger;
import org.openbase.bco.ontology.lib.trigger.TriggerFactory;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import org.slf4j.LoggerFactory;
import rst.domotic.ontology.TriggerConfigType.TriggerConfig;
import rst.domotic.state.ActivationStateType.ActivationState;
import org.openbase.jul.exception.InstantiationException;

/*-
 * #%L
 * BCO Manager Agent Core
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
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
/**
 *
 * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 */
public class OntologyTrigger extends org.openbase.jul.pattern.trigger.AbstractTrigger {

    // TODO: move to ontology
    private Trigger trigger;
    private final Observer<ActivationState.State> triggerObserver;

    public OntologyTrigger(TriggerConfig config) throws InstantiationException {
        super();
        triggerObserver = (Observable<ActivationState.State> source, ActivationState.State data) -> {
            notifyChange(TimestampProcessor.updateTimestampWithCurrentTime(ActivationState.newBuilder().setValue(data).build()));
        };
        try {
            final TriggerFactory triggerFactory = new TriggerFactory();
            trigger = triggerFactory.newInstance(config);
            trigger.addObserver(triggerObserver);
        } catch (CouldNotPerformException | InterruptedException ex) {
            throw new InstantiationException("Could not instantiate OntologyTrigger", ex);
        }
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        trigger.activate();
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        trigger.deactivate();
    }

    @Override
    public boolean isActive() {
        return trigger.isActive();
    }

    @Override
    public void shutdown() {
        trigger.removeObserver(triggerObserver);
        try {
            deactivate();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not shutdown " + this, ex, LoggerFactory.getLogger(getClass()));
        }
        super.shutdown();
    }
}
