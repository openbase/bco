package org.openbase.bco.dal.remote.action;

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
import com.google.protobuf.GeneratedMessage;
import de.citec.csra.allocation.cli.AllocatableResource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.FatalImplementationErrorException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rst.processing.ActionDescriptionProcessor;
import org.openbase.jul.schedule.GlobalScheduledExecutorService;
import org.slf4j.LoggerFactory;
import rsb.RSBException;
import rst.calendar.DateTimeType;
import rst.communicationpatterns.ResourceAllocationType;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.action.MultiResourceAllocationStrategyType.MultiResourceAllocationStrategy;

/**
 *
 * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 */
public class ActionRescheduler {
    //Might be moved somewhere

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ActionRescheduler.class);

    public static enum RescheduleOption {

        EXPIRE, EXTEND
    }

    private final long periodSecs;
    private final Map<String, AllocatableResource> allocationMap;
    private final RescheduleOption rescheduleOption;
    private ScheduledFuture<?> rescheduleFuture;
    private boolean started;

    public ActionRescheduler(RescheduleOption rescheduleOption, long periodSecs) {
        this.allocationMap = new HashMap();
        this.rescheduleOption = rescheduleOption;
        this.periodSecs = periodSecs;
        this.started = false;
    }

    public void startActionRescheduleing(ActionFuture.Builder actionFuture) {
        this.started = true;
        addRescheduleAction(actionFuture);
        if (rescheduleFuture == null && rescheduleOption == RescheduleOption.EXTEND) {
            startExtending();
        }
    }

    public void addRescheduleAction(ActionFuture.Builder actionFuture) {
        if (actionFuture.getActionDescriptionCount() != 0) {
            if (actionFuture.getActionDescription(0).getMultiResourceAllocationStrategy().getStrategy() == MultiResourceAllocationStrategy.Strategy.AT_LEAST_ONE) {
                for (ActionDescription.Builder actionDescription : actionFuture.getActionDescriptionBuilderList()) {
                    try {
                        AllocatableResource allocatableResource = new AllocatableResource(actionDescription.getResourceAllocation());
                        allocatableResource.startup();
                        allocatableResource.getRemote().addSchedulerListener((allocation) -> {
                            switch (allocation.getState()) {
                                case REJECTED:
                                    try {
                                        // rejected because the resource is blocked by someone else
                                        // TODO: is it possible to get information about the allocaiton that currently blocks this
                                        Thread.sleep(500);
                                    } catch (InterruptedException ex) {
                                        Thread.currentThread().interrupt();
                                    }
                                case ABORTED:
                                case RELEASED:
                                case CANCELLED:
                                    reApplyAction(actionDescription, allocation);
                                    break;
                                default:
                                    break;
                            }
                        });
                        switch (allocatableResource.getRemote().getCurrentState()) {
                            case REJECTED:
                            case ABORTED:
                            case RELEASED:
                            case CANCELLED:
                                allocatableResource.getRemote().removeAllSchedulerListeners();
                                reApplyAction(actionDescription, actionDescription.getResourceAllocation());
                                break;
                            default:
                                allocationMap.put(actionDescription.getResourceAllocation().getId(), allocatableResource);
                                break;
                        }
                    } catch (RSBException ex) {
                        ExceptionPrinter.printHistory(ex, LOGGER);
                    }
                }
            } else {
                //TODO ALL_OR_NOTHING strategy
            }
        }
    }

    private void reApplyAction(ActionDescription.Builder actionDescription, ResourceAllocationType.ResourceAllocation allocation) {
        if (!started) {
            return;
        }

        allocationMap.remove(allocation.getId());
        if (rescheduleOption == RescheduleOption.EXTEND) {
            long anHourFromNow = System.currentTimeMillis() + 60 * 60 * 1000;
            DateTimeType.DateTime dateTime = DateTimeType.DateTime.newBuilder().setDateTimeType(DateTimeType.DateTime.Type.FLOATING).setMillisecondsSinceEpoch(anHourFromNow).build();
            actionDescription.setExecutionValidity(dateTime);
        }
        if (actionDescription.getExecutionValidity().getMillisecondsSinceEpoch() > System.currentTimeMillis()) {
            ActionDescriptionProcessor.updateResourceAllocationId(actionDescription);
            ActionDescriptionProcessor.updateResourceAllocationSlot(actionDescription);
            try {
                UnitRemote<? extends GeneratedMessage> unit = Units.getUnit(actionDescription.getServiceStateDescription().getUnitId(), true);
                addRescheduleAction(unit.applyAction(actionDescription.build()).get(actionDescription.getExecutionTimePeriod(), TimeUnit.SECONDS).toBuilder());
                //TODO What to do in case of Exception? Just call reApplyAction again?
            } catch (NotAvailableException ex) {
                ExceptionPrinter.printHistory(ex, LOGGER);
            } catch (CouldNotPerformException | ExecutionException | CancellationException ex) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                reApplyAction(actionDescription, allocation);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } catch (TimeoutException ex) {
            }
        }
    }

    private void startExtending() {
        if (rescheduleOption == RescheduleOption.EXTEND) {
            try {
                rescheduleFuture = GlobalScheduledExecutorService.scheduleAtFixedRate(new Runnable() {
                    @Override
                    public void run() {
                        for (AllocatableResource allocatableResource : allocationMap.values()) {
                            if (allocatableResource.getRemote().getRemainingTime() < (periodSecs / 2 * 1000 * 1000)) {
                                try {
                                    allocatableResource.getRemote().extend(periodSecs, TimeUnit.SECONDS);
                                } catch (RSBException ex) {
                                    ExceptionPrinter.printHistory(new CouldNotPerformException("Could not extend resource allocation", ex), LOGGER);
                                    Logger.getLogger(ActionRescheduler.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                        }
                    }
                }, 0, periodSecs / 3, TimeUnit.SECONDS);
            } catch (NotAvailableException | IllegalArgumentException ex) {
                new FatalImplementationErrorException("Scheduling extension thread failed!", this, ex);
            } catch (RejectedExecutionException ex) {
                ExceptionPrinter.printHistory(ex, LOGGER);
            }
        }
    }

    public void stopExecution() {
        started = false;
        if (rescheduleFuture != null) {
            rescheduleFuture.cancel(true);
            rescheduleFuture = null;
        }
        for (AllocatableResource allocatableResource : allocationMap.values()) {
            allocatableResource.getRemote().removeAllSchedulerListeners();
            try {
                allocatableResource.shutdown();
            } catch (RSBException ex) {
                ExceptionPrinter.printHistory(new CouldNotPerformException("Could not shutdown allocatableResource", ex), LOGGER);
            }
        }
        allocationMap.clear();
    }
}
