package org.dc.bco.manager.scene.binding.openhab.execution;

/*
 * #%L
 * COMA DeviceManager Binding OpenHAB
 * %%
 * Copyright (C) 2015 - 2016 DivineCooperation
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
import org.dc.bco.manager.scene.binding.openhab.transform.ActivationStateTransformer;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.extension.openhab.binding.execution.AbstractOpenHABCommandFactory;
import rst.homeautomation.openhab.OnOffHolderType;
import rst.homeautomation.openhab.OpenhabCommandType;
import rst.homeautomation.openhab.OpenhabCommandType.OpenhabCommand.CommandType;
import rst.homeautomation.state.ActivationStateType.ActivationState;

/**
 *
 * @author mpohling
 */
public class OpenHABCommandFactory extends AbstractOpenHABCommandFactory {

    public static OpenhabCommandType.OpenhabCommand.Builder newOnOffCommand(final ActivationState state) throws CouldNotPerformException {
        return newOnOffCommand(state.getValue());
    }

    public static OpenhabCommandType.OpenhabCommand.Builder newOnOffCommand(final ActivationState.State state) throws CouldNotPerformException {
        return newOnOffCommand(ActivationStateTransformer.transform(state));
    }

    public static OpenhabCommandType.OpenhabCommand.Builder newOnOffCommand(final OnOffHolderType.OnOffHolder state) {
        return getCommandBuilder().setType(CommandType.ONOFF).setOnOff(state);
    }
}
