package org.openbase.bco.app.cloud.connector.google;

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
