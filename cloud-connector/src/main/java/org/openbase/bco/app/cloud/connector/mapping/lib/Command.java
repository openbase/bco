package org.openbase.bco.app.cloud.connector.mapping.lib;

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

    BRIGHTNESS_ABSOLUTE,
    COLOR_ABSOLUTE,
    ON_OFF,
    ACTIVATE_SCENE,
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
        this.representation = COMMAND_PREFIX + StringProcessor.transformUpperCaseToCamelCase(this.name());
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
