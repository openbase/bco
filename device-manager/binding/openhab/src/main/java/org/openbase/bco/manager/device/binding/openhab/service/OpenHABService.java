package org.openbase.bco.manager.device.binding.openhab.service;

/*
 * #%L
 * BCO Manager Device Binding OpenHAB
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

import org.openbase.bco.dal.lib.layer.service.Service;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.bco.manager.device.binding.openhab.DeviceBindingOpenHABImpl;
import org.openbase.bco.manager.device.binding.openhab.transform.ItemNameLoader;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.NotSupportedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.openhab.binding.interfaces.OpenHABRemote;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.schedule.GlobalScheduledExecutorService;
import org.openbase.jul.schedule.SyncObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.binding.openhab.OpenhabCommandType;
import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @param <ST> related service type
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public abstract class OpenHABService<ST extends Service & Unit<?>> implements Service {

    /**
     * first Repeat delay in milliseconds
     */
    public static final int ACTION_EXECUTION_REPEAT_DELAY_1 = 3000;

    /**
     * second Repeat delay in milliseconds
     */
    public static final int ACTION_EXECUTION_REPEAT_DELAY_2 = 10000;
    public static final long ACTION_EXECUTION_TIMEOUT = 15000;

    public static final int REPEAT_TASK_1 = 0;
    public static final int REPEAT_TASK_2 = 1;


    protected final ST unit;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String itemName;
    private final ServiceTemplate.ServiceType serviceType;
    private final ServiceConfig config;
    private final Future[] repeatCommandTasks;
    private final SyncObject repeatLastCommandMonitor = new SyncObject("RepeatLastCommandMonitor");
    protected OpenhabCommandType.OpenhabCommand.Builder lastCommand;
    private OpenHABRemote openHABRemote;

    public OpenHABService(final ST unit) throws InstantiationException {
        try {
            this.unit = unit;
            this.repeatCommandTasks = new Future[2];
            this.serviceType = detectServiceType();
            this.config = loadServiceConfig();
            this.itemName = ItemNameLoader.getItemName(this, config);
            this.openHABRemote = DeviceBindingOpenHABImpl.getInstance().getOpenHABRemote();
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    private ServiceConfig loadServiceConfig() throws CouldNotPerformException {
        for (final ServiceConfig serviceConfig : ((Unit<?>) unit).getConfig().getServiceConfigList()) {
            if (serviceConfig.getServiceDescription().getType().equals(serviceType)) {
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

    public Future executeCommand(final OpenhabCommandType.OpenhabCommand.Builder command) throws CouldNotPerformException {
        if (itemName == null) {
            throw new NotAvailableException("itemID");
        }
        return executeCommand(itemName, command);
    }

    public Future executeCommand(final String itemName, final OpenhabCommandType.OpenhabCommand.Builder command) throws CouldNotPerformException {
        if (command == null) {
            throw new CouldNotPerformException("Skip sending empty command!", new NullPointerException("Argument command is null!"));
        }

        if (openHABRemote == null) {
            throw new CouldNotPerformException("Skip sending command, binding not ready!", new NullPointerException("Argument rsbBinding is null!"));
        }

        logger.debug("Execute command: Setting item [" + this.itemName + "] to [" + command.getType().toString() + "]");
        return openHABRemote.postCommand(command.setItem(itemName).build());
    }


    /**
     * Method repeats the given command in 5 seconds.
     * Make sure the last command is always stored into the {@code lastCommand} variable.
     */
    public void repeatLastCommand() {
        synchronized (repeatLastCommandMonitor) {

            // cancel still running tasks
            if (repeatCommandTasks[REPEAT_TASK_1] != null && !repeatCommandTasks[REPEAT_TASK_1].isDone()) {
                // cancel if still scheduled but do to cancel if already executing.
                repeatCommandTasks[REPEAT_TASK_1].cancel(false);
            }
            if (repeatCommandTasks[REPEAT_TASK_2] != null && !repeatCommandTasks[REPEAT_TASK_2].isDone()) {
                // cancel if still scheduled but do to cancel if already executing.
                repeatCommandTasks[REPEAT_TASK_2].cancel(false);
            }

            // this is just a bug workaround because the philip hues are sometimes skip events.
            // So to make sure they are controlled like expected we repeat the command twice.

            // init repeat 1
            repeatCommandTasks[REPEAT_TASK_1] = GlobalScheduledExecutorService.schedule(() -> {
                try {
                    executeCommand(lastCommand).get(ACTION_EXECUTION_TIMEOUT, TimeUnit.SECONDS);
                    logger.info("repeat successfully command[" + lastCommand + "]");
                } catch (Exception ex) {
                    ExceptionPrinter.printHistory("Could not repeat openhab command!", ex, logger);
                }
            }, ACTION_EXECUTION_REPEAT_DELAY_1, TimeUnit.MILLISECONDS);

            // init repeat 2
            repeatCommandTasks[REPEAT_TASK_2] = GlobalScheduledExecutorService.schedule(() -> {
                try {
                    executeCommand(lastCommand).get(ACTION_EXECUTION_TIMEOUT, TimeUnit.SECONDS);
                    logger.info("repeat successfully command[" + lastCommand + "]");
                } catch (Exception ex) {
                    ExceptionPrinter.printHistory("Could not repeat openhab command!", ex, logger);
                }
            }, ACTION_EXECUTION_REPEAT_DELAY_2, TimeUnit.MILLISECONDS);
        }
    }
}
