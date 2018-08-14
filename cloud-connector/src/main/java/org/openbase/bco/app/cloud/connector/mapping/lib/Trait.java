package org.openbase.bco.app.cloud.connector.mapping.lib;

/*-
 * #%L
 * BCO Cloud Connector
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

import com.google.gson.JsonObject;
import org.openbase.bco.app.cloud.connector.mapping.service.ColorStateColorSpectrumMapper;
import org.openbase.bco.app.cloud.connector.mapping.service.ColorStateColorTemperatureMapper;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.processing.StringProcessor;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Enum mapping trait types by Google.
 * This enum also includes which commands can be send for which trait and a mapper used to
 * map json data send for this trait to a service state.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public enum Trait {

    BRIGHTNESS(Command.BRIGHTNESS_ABSOLUTE),
    COLOR_SPECTRUM(Command.COLOR_ABSOLUTE),
    COLOR_TEMPERATURE(Command.COLOR_ABSOLUTE),
    MODES(Command.SET_MODES),
    ON_OFF(Command.ON_OFF),
    SCENE(Command.ACTIVATE_SCENE),
    TEMPERATURE_SETTING(Command.THERMOSTAT_TEMPERATURE_SETPOINT, Command.THERMOSTAT_SET_MODE),
    TOGGLES(Command.SET_TOGGLES);

    public static final String REPRESENTATION_PREFIX = "action.devices.traits.";

    private final String representation;
    private final Set<Command> commandSet;

    Trait(final Command... commands) {
        this.representation = REPRESENTATION_PREFIX + StringProcessor.transformUpperCaseToCamelCase(this.name());
        this.commandSet = new HashSet<>(Arrays.asList(commands));
    }

    public String getRepresentation() {
        return representation;
    }

    public Set<Command> getCommandSet() {
        return Collections.unmodifiableSet(commandSet);
    }

    public static Trait getByCommand(final Command command, final JsonObject params) throws NotAvailableException {
        if (command == Command.COLOR_ABSOLUTE) {
            JsonObject color = params.getAsJsonObject(ColorStateColorSpectrumMapper.COLOR_KEY);
            if (color.has(ColorStateColorTemperatureMapper.TEMPERATURE_KEY)) {
                return COLOR_TEMPERATURE;
            } else if (color.has(ColorStateColorSpectrumMapper.COLOR_MODEL_HSB)) {
                return COLOR_SPECTRUM;
            }
        }


        for (final Trait trait : Trait.values()) {
            for (final Command traitCommand : trait.getCommandSet()) {
                if (traitCommand == command) {
                    return trait;
                }
            }
        }
        throw new NotAvailableException("Trait with command[" + command.name() + "]");
    }
}
