package org.openbase.bco.manager.agent.core;

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
import com.google.protobuf.GeneratedMessage;
import de.citec.csra.allocation.cli.AllocatableResource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rst.processing.ActionDescriptionProcessor;
import org.openbase.jul.schedule.GlobalScheduledExecutorService;
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
public class AgentActionRescheduleHelper {
    //Might be moved somewhere as a general ActionRescheduleHelper

    public static enum RescheduleOption {
        EXPIRE, EXTEND
    }

    private final long periodSecs;
    private final Map<String, AllocatableResource> allocationMap;
    private final RescheduleOption rescheduleOption;
    private ScheduledFuture<?> rescheduleFuture;

    public AgentActionRescheduleHelper(RescheduleOption rescheduleOption, long periodSecs) {
        this.allocationMap = new HashMap();
        this.rescheduleOption = rescheduleOption;
        this.periodSecs = periodSecs;
    }

    public void startActionRescheduleing(ActionFuture.Builder actionFuture) {
        if (rescheduleFuture == null && rescheduleOption == RescheduleOption.EXTEND) {
            startExtending();
        }
        addRescheduleAction(actionFuture);
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
                                        Thread.sleep(1000);
                                    } catch (InterruptedException ex) {
//                                        Logger.getLogger(PresenceLightAgent.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                case ABORTED:
                                case RELEASED:
                                case CANCELLED:
                                    System.out.println("ActionDescription " + actionDescription.getDescription());
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
                        Logger.getLogger(AgentActionRescheduleHelper.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } else {
                //TODO ALL_OR_NOTHING strategy
            }
        }
    }

    private void reApplyAction(ActionDescription.Builder actionDescription, ResourceAllocationType.ResourceAllocation allocation) {
        allocationMap.remove(allocation.getId());
        if (rescheduleOption == RescheduleOption.EXTEND) {
            long anHourFromNow = System.currentTimeMillis() + 60 * 60 * 1000;
            DateTimeType.DateTime dateTime = DateTimeType.DateTime.newBuilder().setDateTimeType(DateTimeType.DateTime.Type.FLOATING).setMillisecondsSinceEpoch(anHourFromNow).build();
            actionDescription.setExecutionValidity(dateTime);
        }
        System.out.println("Validity: " + actionDescription.getExecutionValidity().getMillisecondsSinceEpoch() + " current " + System.currentTimeMillis());
        if (actionDescription.getExecutionValidity().getMillisecondsSinceEpoch() > System.currentTimeMillis()) {
            System.out.println("Inside boundaries");
            ActionDescriptionProcessor.updateResourceAllocationId(actionDescription);
            ActionDescriptionProcessor.updateResourceAllocationSlot(actionDescription);
            try {
                UnitRemote<? extends GeneratedMessage> unit = Units.getUnit(actionDescription.getServiceStateDescription().getUnitId(), true);
                addRescheduleAction(unit.applyAction(actionDescription.build()).get().toBuilder());
                //TODO What to do in case of Exception? Just call reApplyAction again?
            } catch (NotAvailableException ex) {
                Logger.getLogger(AgentActionRescheduleHelper.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InterruptedException | CouldNotPerformException | ExecutionException | CancellationException ex) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
                reApplyAction(actionDescription, allocation);
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
                                    Logger.getLogger(AgentActionRescheduleHelper.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                        }
                    }
                }, 0, periodSecs / 3, TimeUnit.SECONDS);
            } catch (NotAvailableException ex) {
                Logger.getLogger(AgentActionRescheduleHelper.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(AgentActionRescheduleHelper.class.getName()).log(Level.SEVERE, null, ex);
            } catch (RejectedExecutionException ex) {
                Logger.getLogger(AgentActionRescheduleHelper.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void stopExecution() {
        if (rescheduleFuture != null) {
            rescheduleFuture.cancel(true);
            rescheduleFuture = null;
        }
        for (AllocatableResource allocatableResource : allocationMap.values()) {
            allocatableResource.getRemote().removeAllSchedulerListeners();
            try {
                allocatableResource.shutdown();
            } catch (RSBException ex) {
                Logger.getLogger(AgentActionRescheduleHelper.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        allocationMap.clear();
    }
}
