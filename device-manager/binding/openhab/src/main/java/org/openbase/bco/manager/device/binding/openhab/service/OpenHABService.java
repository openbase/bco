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
import java.util.concurrent.Future;
import org.openbase.bco.dal.lib.layer.service.Service;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.bco.manager.device.binding.openhab.DeviceBindingOpenHABImpl;
import org.openbase.bco.manager.device.binding.openhab.transform.ItemNameLoader;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.NotSupportedException;
import org.openbase.jul.extension.openhab.binding.interfaces.OpenHABRemote;
import org.openbase.jul.processing.StringProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.binding.openhab.OpenhabCommandType;
import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 * @param <ST> related service type
 */
public abstract class OpenHABService<ST extends Service & Unit<?>> implements Service {

    private OpenHABRemote openHABRemote;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected final ST unit;
    private final String itemName;
    private final ServiceTemplate.ServiceType serviceType;
    private final ServiceConfig config;

    public OpenHABService(final ST unit) throws InstantiationException {
        try {
            this.unit = unit;
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
}
