package org.openbase.bco.app.cloudconnector.mapping.lib;

/*-
 * #%L
 * BCO Cloud Connector
 * %%
 * Copyright (C) 2018 - 2020 openbase.org
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

import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.processing.StringProcessor;

import java.util.HashMap;
import java.util.Map;

/**
 * Enum mapping command types for traits by Google.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public enum Command {

    ACTIVATE_SCENE,
    BRIGHTNESS_ABSOLUTE,
    COLOR_ABSOLUTE,
    ON_OFF,
    SET_MODES,
    SET_TOGGLES,
    THERMOSTAT_SET_MODE,
    THERMOSTAT_TEMPERATURE_SETPOINT;

    /**
     * Prefix used to build the representation of a command.
     */
    public static final String COMMAND_PREFIX = "action.devices.commands.";

    // create and fill mapping of representation to command
    private static final Map<String, Command> commandRepresentationMap = new HashMap<>();
    static {
        for (final Command command : Command.values()) {
            commandRepresentationMap.put(command.getRepresentation(), command);
        }
    }

    private final String representation;

    Command() {
        this.representation = COMMAND_PREFIX + StringProcessor.transformUpperCaseToPascalCase(this.name());
    }

    /**
     * Get a string representation as sent for this commands.
     * This is done by converting the name to camel case and then adding a prefix.
     * E.g. COLOR_ABSOLUTE becomes action.devices.commands.ColorAbsolute
     *
     * @return a representation as sent for this command
     */
    public String getRepresentation() {
        return representation;
    }

    /**
     * Get a command by its representation.
     *
     * @param commandRepresentation the representation as returned by getRepresentation for a Command
     * @return the command having the given representation
     * @throws NotAvailableException if no command with the given representation exists
     */
    public static Command getByRepresentation(final String commandRepresentation) throws NotAvailableException {
        if (commandRepresentationMap.containsKey(commandRepresentation)) {
            return commandRepresentationMap.get(commandRepresentation);
        }
        throw new NotAvailableException("Command with representation[" + commandRepresentation + "]");
    }
}
