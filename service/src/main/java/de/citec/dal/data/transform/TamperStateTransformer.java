/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.data.transform;

import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.exception.TypeNotSupportedException;
import org.openhab.core.library.types.DecimalType;
import rst.homeautomation.states.TamperType;

/**
 *
 * @author thuxohl
 */
public class TamperStateTransformer {

	public static TamperType.Tamper.TamperState transform(DecimalType decimalType) throws RSBBindingException {
		switch (decimalType.intValue()) {
			case 0:
				return TamperType.Tamper.TamperState.NO_TAMPER;
			case 1:
				return TamperType.Tamper.TamperState.TAMPER;
			default:
				throw new RSBBindingException("Could not transform " + DecimalType.class.getName() + "! " + DecimalType.class.getSimpleName() + "[" + decimalType.intValue() + "] is unknown!");
		}
	}

	public static DecimalType transform(TamperType.Tamper.TamperState tamperState) throws TypeNotSupportedException, RSBBindingException {
		switch (tamperState) {
			case NO_TAMPER:
				return new DecimalType(0);
			case TAMPER:
				return new DecimalType(1);
			case UNKNOWN:
				throw new TypeNotSupportedException(tamperState, DecimalType.class);
			default:
				throw new RSBBindingException("Could not transform " + TamperType.Tamper.TamperState.class.getName() + "! " + TamperType.Tamper.TamperState.class.getSimpleName() + "[" + tamperState.name() + "] is unknown!");
		}
	}
}
