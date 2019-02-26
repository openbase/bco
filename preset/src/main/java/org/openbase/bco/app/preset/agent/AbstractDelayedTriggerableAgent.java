package org.openbase.bco.app.preset.agent;

/*-
 * #%L
 * BCO App Preset
 * %%
 * Copyright (C) 2018 - 2019 openbase.org
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

import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.schedule.Timeout;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState;

import java.util.concurrent.ExecutionException;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public abstract class AbstractDelayedTriggerableAgent extends AbstractTriggerableAgent {

    private final Timeout timeout;
    private final long minDelay;
    private final long maxDelay;
    private final DelayMode delayMode;
    private ActivationState lastActivationState;


    public enum DelayMode {
        DELAY_ACTIVATION,
        DELAY_DEACTIVATION
    }

    /**
     * Constructor of this class.
     * <p>
     * Note: the default min delay is 0
     *
     * @param delayMode mode defines which trigger event is delayed.
     * @param maxDelay  the maximal time to delay.
     *
     * @throws InstantiationException is throw if the instance could not be created.
     */
    public AbstractDelayedTriggerableAgent(final DelayMode delayMode, final long maxDelay) throws InstantiationException {
        this(delayMode, 0, maxDelay);
    }

    /**
     * Constructor of this class.
     *
     * @param delayMode mode defines which trigger event is delayed.
     * @param minDelay  the minimal time to delay.
     * @param maxDelay  the maximal time to delay.
     *
     * @throws InstantiationException is throw if the instance could not be created.
     */
    public AbstractDelayedTriggerableAgent(final DelayMode delayMode, final long minDelay, final long maxDelay) throws InstantiationException {
        this.delayMode = delayMode;
        this.minDelay = minDelay;
        this.maxDelay = maxDelay;

        // Use min delay as initial timeout because the delay scale factor might not be available at this stage.
        // Once the timeout is started, the value will recomputed anyway.
        this.timeout = new Timeout(minDelay) {

            @Override
            public void expired() {
                try {
                    delayedTrigger(lastActivationState);
                } catch (InterruptedException ex) {
                    return;
                } catch (CouldNotPerformException | ExecutionException ex) {
                    ExceptionPrinter.printHistory("Could not trigger agent after delay!", ex, logger);
                }
            }
        };
    }

    /**
     * Scale the delay related to the delay scale.
     * The min value is always guaranteed while the delta between max and min timeout is scaled by the timescale.
     * Method can be overloaded to change its default behaviour.
     * <p>
     * Note: Be aware that the default implementation returns 0 during junit tests and the average timeout in case the scale value is not available.
     *
     * @return the computed timeout in ms.
     */
    protected long computeDelay() {
        final long delta = maxDelay - minDelay;
        try {
            return JPService.testMode() ? 0 : minDelay + (long) (delta * getDelayScaleFactor());
        } catch (NotAvailableException e) {
            return minDelay + (long) (delta * 0.5d);
        }
    }

    @Override
    protected final void trigger(final ActivationState activationState) throws CouldNotPerformException, ExecutionException, InterruptedException {
        switch (delayMode) {
            case DELAY_ACTIVATION:
                switch (activationState.getValue()) {
                    case ACTIVE:
                        this.lastActivationState = activationState;
                        timeout.start(computeDelay());
                        break;
                    case DEACTIVE:
                        timeout.cancel();
                        delayedTrigger(activationState);
                        break;
                }
                break;
            case DELAY_DEACTIVATION:
                switch (activationState.getValue()) {
                    case ACTIVE:
                        timeout.cancel();
                        delayedTrigger(activationState);
                        break;
                    case DEACTIVE:
                        this.lastActivationState = activationState;
                        timeout.start(computeDelay());
                        break;
                }
                break;
        }
    }

    /**
     * Depending on the chosen delay mode the activation or deactivation is delayed before this method forwards the updated state.
     * <p>
     * Node: the time to delay relative to the current timeout scale.
     *
     * @param activationState the forwarded or delayed state.
     *
     * @throws CouldNotPerformException can be used to inform that the triggered action has failed.
     * @throws ExecutionException       can be used to inform that the triggered action has failed.
     * @throws InterruptedException     can be used to forward an external thread interruption.
     */
    abstract protected void delayedTrigger(final ActivationState activationState) throws CouldNotPerformException, ExecutionException, InterruptedException;

    /**
     * Method should return a normalized scale factor between 0.0 - 1.0
     * which is used to dynamically scale the delay.
     * <p>
     * Note: Agents can use there emphasis category values of its parent location to scale the delay.
     * Therefor no individual agent configuration is needed but the behaviour of agents can still be configured via the user emphasis.
     */
    abstract protected double getDelayScaleFactor() throws NotAvailableException;
}
