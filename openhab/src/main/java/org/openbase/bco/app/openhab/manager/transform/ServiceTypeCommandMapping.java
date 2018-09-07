package org.openbase.bco.app.openhab.manager.transform;

/*-
 * #%L
 * BCO Openhab App
 * %%
 * Copyright (C) 2018 openbase.org
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

import org.eclipse.smarthome.core.library.types.*;
import org.eclipse.smarthome.core.types.Command;
import org.openbase.jul.exception.NotAvailableException;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public enum ServiceTypeCommandMapping {

    COLOR_STATE_SERVICE_COMMAND_MAPPING(HSBType.class),
    POWER_STATE_SERVICE_COMMAND_MAPPING(OnOffType.class),
    BRIGHTNESS_STATE_SERVICE_COMMAND_MAPPING(PercentType.class),
    POWER_CONSUMPTION_STATE_SERVICE_COMMAND_MAPPING(DecimalType.class),
    TEMPERATURE_STATE_SERVICE_COMMAND_MAPPING(DecimalType.class),
    MOTION_STATE_SERVICE_COMMAND_MAPPING(DecimalType.class),
    TAMPER_STATE_SERVICE_COMMAND_MAPPING(DecimalType.class),
    BATTERY_STATE_SERVICE_COMMAND_MAPPING(DecimalType.class),
    SMOKE_ALARM_STATE_SERVICE_COMMAND_MAPPING(DecimalType.class),
    SMOKE_STATE_SERVICE_COMMAND_MAPPING(DecimalType.class),
    TEMPERATURE_ALARM_STATE_SERVICE_COMMAND_MAPPING(DecimalType.class),
    TARGET_TEMPERATURE_STATE_SERVICE_COMMAND_MAPPING(DecimalType.class),
    ILLUMINANCE_STATE_SERVICE_COMMAND_MAPPING(DecimalType.class),
    BLIND_STATE_SERVICE_COMMAND_MAPPING(UpDownType.class, StopMoveType.class, PercentType.class),
    BUTTON_STATE_SERVICE_COMMAND_MAPPING(OnOffType.class),
    CONTACT_STATE_SERVICE_COMMAND_MAPPING(OpenClosedType.class),
    HANDLE_STATE_SERVICE_COMMAND_MAPPING(StringType.class),
    STANDBY_STATE_SERVICE_COMMAND_MAPPING(OnOffType.class),
    ACTIVATION_STATE_SERVICE_COMMAND_MAPPING(OnOffType.class);

    private static final String POSTFIX = "_COMMAND_MAPPING";

    private final ServiceType serviceType;
    private final Set<Class<? extends Command>> commandClasses;

    ServiceTypeCommandMapping(final Class<? extends Command>... commandClasses) {
        this.serviceType = ServiceType.valueOf(name().replace(POSTFIX, ""));
        this.commandClasses = new HashSet<>();
        this.commandClasses.addAll(Arrays.asList(commandClasses));
    }

    public static ServiceTypeCommandMapping fromServiceType(final ServiceType serviceType) throws NotAvailableException {
        for (final ServiceTypeCommandMapping value : ServiceTypeCommandMapping.values()) {
            if (value.getServiceType() == serviceType) {
                return value;
            }
        }
        throw new NotAvailableException("ServiceTypeCommandMapping for " + serviceType.name());
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public Set<Class<? extends Command>> getCommandClasses() {
        return commandClasses;
    }
}
