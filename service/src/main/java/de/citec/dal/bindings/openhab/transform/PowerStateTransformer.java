/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab.transform;

import org.dc.jul.exception.CouldNotTransformException;
import org.dc.jul.exception.TypeNotSupportedException;
import rst.homeautomation.state.PowerStateType.PowerState;
import rst.homeautomation.openhab.OnOffHolderType;

/**
 *
 * @author mpohling
 */
public class PowerStateTransformer {

	public static PowerState.State transform(OnOffHolderType.OnOffHolder.OnOff onOffType) throws CouldNotTransformException {
		switch (onOffType) {
			case OFF:
				return PowerState.State.OFF;
			case ON:
				return PowerState.State.ON;
			default:
				throw new CouldNotTransformException("Could not transform " + OnOffHolderType.OnOffHolder.OnOff.class.getName() + "! " + OnOffHolderType.OnOffHolder.OnOff.class.getSimpleName() + "[" + onOffType.name() + "] is unknown!");
		}
	}

	public static OnOffHolderType.OnOffHolder transform(PowerState.State powerState) throws TypeNotSupportedException, CouldNotTransformException {
		switch (powerState) {
			case OFF:
				return OnOffHolderType.OnOffHolder.newBuilder().setState(OnOffHolderType.OnOffHolder.OnOff.OFF).build();
			case ON:
				return OnOffHolderType.OnOffHolder.newBuilder().setState(OnOffHolderType.OnOffHolder.OnOff.ON).build();
			case UNKNOWN:
				throw new TypeNotSupportedException(powerState, OnOffHolderType.OnOffHolder.OnOff.class);
			default:
				throw new CouldNotTransformException("Could not transform " + PowerState.State.class.getName() + "! " + PowerState.State.class.getSimpleName() + "[" + powerState.name() + "] is unknown!");
		}
	}
}
