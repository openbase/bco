package org.openbase.bco.app.cloud.connector.google;

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

import org.openbase.bco.app.cloud.connector.google.mapping.state.*;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.processing.StringProcessor;

public enum Trait {

    BRIGHTNESS("action.devices.commands.BrightnessAbsolute", new BrightnessTraitMapper()),
    COLOR_SPECTRUM("action.devices.commands.ColorAbsolute", new ColorSpectrumTraitMapper()),
    COLOR_TEMPERATURE("action.devices.commands.ColorAbsolute", new ColorTemperatureTraitMapper()),
    ON_OFF("action.devices.commands.OnOff", new OnOffTraitMapper());

    public static final String REPRESENTATION_PREFIX = "action.devices.traits.";

    private final String representation;
    private final TraitMapper traitMapper;
    private final String command;

    Trait(final String command, final TraitMapper traitMapper) {
        this.representation = REPRESENTATION_PREFIX + StringProcessor.transformUpperCaseToCamelCase(this.name());
        this.traitMapper = traitMapper;
        this.command = command;
    }

    public String getRepresentation() {
        return representation;
    }

    public TraitMapper getTraitMapper() {
        return traitMapper;
    }

    public String getCommand() {
        return command;
    }

    public static Trait getByCommand(final String command) throws NotAvailableException {
        for (final Trait trait : Trait.values()) {
            if (trait.getCommand().equals(command)) {
                return trait;
            }
        }
        throw new NotAvailableException("Trait with command[" + command + "]");
    }
}
