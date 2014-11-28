/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.data.transform;

import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.exception.TypeNotSupportedException;
import rst.homeautomation.states.PowerType;
import rst.homeautomation.openhab.OnOffHolderType;

/**
 *
 * @author mpohling
 */
public class PowerStateTransformer {

	public static PowerType.Power.PowerState transform(OnOffHolderType.OnOffHolder.OnOff onOffType) throws RSBBindingException {
		switch (onOffType) {
			case OFF:
				return PowerType.Power.PowerState.OFF;
			case ON:
				return PowerType.Power.PowerState.ON;
			default:
				throw new RSBBindingException("Could not transform " + OnOffHolderType.OnOffHolder.OnOff.class.getName() + "! " + OnOffHolderType.OnOffHolder.OnOff.class.getSimpleName() + "[" + onOffType.name() + "] is unknown!");
		}
	}

	public static OnOffHolderType.OnOffHolder transform(PowerType.Power.PowerState powerState) throws TypeNotSupportedException, RSBBindingException {
		switch (powerState) {
			case OFF:
				return OnOffHolderType.OnOffHolder.newBuilder().setState(OnOffHolderType.OnOffHolder.OnOff.OFF).build();
			case ON:
				return OnOffHolderType.OnOffHolder.newBuilder().setState(OnOffHolderType.OnOffHolder.OnOff.ON).build();
			case UNKNOWN:
				throw new TypeNotSupportedException(powerState, OnOffHolderType.OnOffHolder.OnOff.class);
			default:
				throw new RSBBindingException("Could not transform " + PowerType.Power.PowerState.class.getName() + "! " + PowerType.Power.PowerState.class.getSimpleName() + "[" + powerState.name() + "] is unknown!");
		}
	}
}
