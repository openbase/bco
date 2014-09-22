/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.data.transform;

import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.exception.TypeNotSupportedException;
import org.openhab.core.library.types.OnOffType;
import rst.homeautomation.states.PowerType;

/**
 *
 * @author mpohling
 */
public class PowerStateTransformer {

	public static PowerType.Power.PowerState transform(OnOffType onOffType) throws RSBBindingException {
		switch (onOffType) {
			case OFF:
				return PowerType.Power.PowerState.OFF;
			case ON:
				return PowerType.Power.PowerState.ON;
			default:
				throw new RSBBindingException("Could not transform " + OnOffType.class.getName() + "! " + OnOffType.class.getSimpleName() + "[" + onOffType.name() + "] is unknown!");
		}
	}

	public static OnOffType transform(PowerType.Power.PowerState powerState) throws TypeNotSupportedException, RSBBindingException {
		switch (powerState) {
			case OFF:
				return OnOffType.OFF;
			case ON:
				return OnOffType.ON;
			case UNKNOWN:
				throw new TypeNotSupportedException(powerState, OnOffType.class);
			default:
				throw new RSBBindingException("Could not transform " + PowerType.Power.PowerState.class.getName() + "! " + PowerType.Power.PowerState.class.getSimpleName() + "[" + powerState.name() + "] is unknown!");
		}
	}
}
