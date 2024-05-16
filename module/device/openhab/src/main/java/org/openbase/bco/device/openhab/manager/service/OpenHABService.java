package org.openbase.bco.device.openhab.manager.service;

/*-
 * #%L
 * BCO Openhab Device Manager
 * %%
 * Copyright (C) 2015 - 2021 openbase.org
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
import org.openbase.bco.dal.lib.layer.service.Service;
import org.openbase.bco.dal.lib.layer.service.ServiceProvider;
import org.openbase.bco.dal.lib.layer.service.ServiceStateProcessor;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.bco.device.openhab.communication.OpenHABRestCommunicator;
import org.openbase.bco.device.openhab.manager.transform.ServiceStateCommandTransformerPool;
import org.openbase.bco.device.openhab.manager.transform.ServiceTypeCommandMapping;
import org.openbase.bco.device.openhab.registry.synchronizer.OpenHABItemProcessor;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.service.ServiceConfigType.ServiceConfig;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;

public abstract class OpenHABService<ST extends Service & Unit<?>> implements Service {

    protected final ST unit;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String itemName;
    private final ServiceTemplate.ServiceType serviceType;

    public OpenHABService(final ST unit) throws InstantiationException {
        try {
            this.unit = unit;
            this.serviceType = detectServiceType();

            loadServiceConfig();

            this.itemName = OpenHABItemProcessor.generateItemName(unit.getConfig(), serviceType);
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    private void loadServiceConfig() throws CouldNotPerformException {
        for (final ServiceConfig serviceConfig : unit.getConfig().getServiceConfigList()) {
            if (serviceConfig.getServiceDescription().getServiceType().equals(serviceType)) {
                return;
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

    public Future<ActionDescription> setState(final Message serviceState) {
        try {
            boolean success = false;
            MultiException.ExceptionStack exceptionStack = null;
            for (final Class<? extends Command> commandClass : ServiceTypeCommandMapping.getCommandClasses(serviceType)) {
                Command command = ServiceStateCommandTransformerPool.getInstance().getTransformer(serviceState.getClass(), commandClass).transform(serviceState);
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
                    ExceptionPrinter.printHistory(ex, logger, LogLevel.WARN);
                }
            }

            return FutureProcessor.completedFuture(ServiceStateProcessor.getResponsibleAction(serviceState, ActionDescription::getDefaultInstance));
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, ex);
        }
    }

    @Override
    public ServiceProvider getServiceProvider() {
        return unit;
    }
}
