package org.openbase.bco.device.openhab.manager.service;

/*-
 * #%L
 * BCO Openhab Device Manager
 * %%
 * Copyright (C) 2015 - 2020 openbase.org
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

import com.google.protobuf.Message;
import org.eclipse.smarthome.core.types.Command;
import org.openbase.bco.device.openhab.OpenHABRestCommunicator;
import org.openbase.bco.device.openhab.manager.transform.ServiceStateCommandTransformerPool;
import org.openbase.bco.device.openhab.manager.transform.ServiceTypeCommandMapping;
import org.openbase.bco.device.openhab.registry.synchronizer.OpenHABItemProcessor;
import org.openbase.bco.dal.lib.layer.service.Service;
import org.openbase.bco.dal.lib.layer.service.ServiceProvider;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.jul.schedule.SyncObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.binding.openhab.OpenhabCommandType;
import org.openbase.type.domotic.service.ServiceConfigType.ServiceConfig;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate;

import java.util.concurrent.Future;

public abstract class OpenHABService<ST extends Service & Unit<?>> implements Service {

//    /**
//     * first Repeat delay in milliseconds
//     */
//    public static final int ACTION_EXECUTION_REPEAT_DELAY_1 = 3000;
//
//    /**
//     * second Repeat delay in milliseconds
//     */
//    public static final int ACTION_EXECUTION_REPEAT_DELAY_2 = 10000;
//    public static final long ACTION_EXECUTION_TIMEOUT = 15000;
//
//    public static final int REPEAT_TASK_1 = 0;
//    public static final int REPEAT_TASK_2 = 1;


    protected final ST unit;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String itemName;
    private final ServiceTemplate.ServiceType serviceType;
    private final ServiceConfig config;
    private final Future[] repeatCommandTasks;
    private final SyncObject repeatLastCommandMonitor = new SyncObject("RepeatLastCommandMonitor");
    protected OpenhabCommandType.OpenhabCommand.Builder lastCommand;

    public OpenHABService(final ST unit) throws InstantiationException {
        try {
            this.unit = unit;
            this.repeatCommandTasks = new Future[2];
            this.serviceType = detectServiceType();
            this.config = loadServiceConfig();
            this.itemName = OpenHABItemProcessor.generateItemName(unit.getConfig(), serviceType);
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    private ServiceConfig loadServiceConfig() throws CouldNotPerformException {
        for (final ServiceConfig serviceConfig : unit.getConfig().getServiceConfigList()) {
            if (serviceConfig.getServiceDescription().getServiceType().equals(serviceType)) {
                return serviceConfig;
            }
        }
        throw new CouldNotPerformException("Could not detect service config! Service[" + serviceType.name() + "] is not configured in Unit[" + ((Unit) unit).getId() + "]!");
    }

    public final ServiceTemplate.ServiceType detectServiceType() throws NotSupportedException {
        return ServiceTemplate.ServiceType.valueOf(StringProcessor.transformToUpperCase(getClass().getSimpleName().replaceFirst("Impl", "")));
    }

    public ST getUnit() {
        return unit;
    }

    public String getItemID() {
        return itemName;
    }

    public Future<ActionDescription> setState(final Message state) {
        try {
            boolean success = false;
            MultiException.ExceptionStack exceptionStack = null;
            for (final Class<? extends Command> commandClass : ServiceTypeCommandMapping.getCommandClasses(serviceType)) {
                Command command = ServiceStateCommandTransformerPool.getInstance().getTransformer(state.getClass(), commandClass).transform(state);
                try {
                    OpenHABRestCommunicator.getInstance().postCommand(itemName, command.toString());
                    success = true;
                } catch (CouldNotPerformException ex) {
                    if (ex.getCause() instanceof NotAvailableException) {
                        throw new CouldNotPerformException("Thing may not be configured or openHAB not reachable", ex);
                    }
                    exceptionStack = MultiException.push("Could not apply state via " + commandClass.getSimpleName() + "!", ex, exceptionStack);
                }
            }

            if (!success) {
                MultiException.checkAndThrow(() -> "Could not apply state!", exceptionStack);
            } else {
                // if at least one command reached its target, then we just print a warning
                try {
                    MultiException.checkAndThrow(() -> "Some command classes could not be used to apply the state:", exceptionStack);
                } catch (Exception ex) {
                    ExceptionPrinter.printHistory(ex , logger, LogLevel.WARN);
                }
            }

            // todo build proper action description instead returning null.
            return FutureProcessor.completedFuture(null);
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, ex);
        }
    }

//    public Future<ActionDescription> executeCommand(final Command... commands) {
//        if (itemName == null) {
//            throw new NotAvailableException("itemID");
//        }
//
//        try {
//            for (final Command command : commands) {
//                OpenHABRestCommunicator.getInstance().postCommand(itemName, command.toString());
//            }
//        } catch (CouldNotPerformException ex) {
//            if (ex.getCause() instanceof NotAvailableException) {
//                throw new CouldNotPerformException("Thing may not be configured or openHAB not reachable", ex);
//            }
//            throw ex;
//        }
//        return FutureProcessor.completedFuture(null);
//    }

    @Override
    public ServiceProvider getServiceProvider() {
        return unit;
    }

    /**
     * Method repeats the given command in 5 seconds.
     * Make sure the last command is always stored into the {@code lastCommand} variable.
     */
//        public void repeatLastCommand() {
//            synchronized (repeatLastCommandMonitor) {
//
//                // cancel still running tasks
//                if (repeatCommandTasks[REPEAT_TASK_1] != null && !repeatCommandTasks[REPEAT_TASK_1].isDone()) {
//                    // cancel if still scheduled but do to cancel if already executing.
//                    repeatCommandTasks[REPEAT_TASK_1].cancel(false);
//                }
//                if (repeatCommandTasks[REPEAT_TASK_2] != null && !repeatCommandTasks[REPEAT_TASK_2].isDone()) {
//                    // cancel if still scheduled but do to cancel if already executing.
//                    repeatCommandTasks[REPEAT_TASK_2].cancel(false);
//                }
//
//                // this is just a bug workaround because the philip hues are sometimes skip events.
//                // So to make sure they are controlled like expected we repeat the command twice.
//
//                // init repeat 1
//                repeatCommandTasks[REPEAT_TASK_1] = GlobalScheduledExecutorService.schedule(() -> {
//                    try {
//                        executeCommand(lastCommand).get(ACTION_EXECUTION_TIMEOUT, TimeUnit.SECONDS);
//                        logger.info("repeat successfully command[" + lastCommand + "]");
//                    } catch (Exception ex) {
//                        ExceptionPrinter.printHistory("Could not repeat openhab command!", ex, logger);
//                    }
//                }, ACTION_EXECUTION_REPEAT_DELAY_1, TimeUnit.MILLISECONDS);
//
//                // init repeat 2
//                repeatCommandTasks[REPEAT_TASK_2] = GlobalScheduledExecutorService.schedule(() -> {
//                    try {
//                        executeCommand(lastCommand).get(ACTION_EXECUTION_TIMEOUT, TimeUnit.SECONDS);
//                        logger.info("repeat successfully command[" + lastCommand + "]");
//                    } catch (Exception ex) {
//                        ExceptionPrinter.printHistory("Could not repeat openhab command!", ex, logger);
//                    }
//                }, ACTION_EXECUTION_REPEAT_DELAY_2, TimeUnit.MILLISECONDS);
//            }
//        }
}
