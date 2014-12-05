/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.data.transform;

import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.exception.TypeNotSupportedException;
import rst.homeautomation.states.TamperType;
import rst.homeautomation.openhab.DecimalType;

/**
 *
 * @author thuxohl
 */
public class TamperStateTransformer {

	public static TamperType.Tamper.TamperState transform(double decimalType) throws RSBBindingException {
		switch ((int) decimalType) {
			case 0:
				return TamperType.Tamper.TamperState.NO_TAMPER;
			case 1:
				return TamperType.Tamper.TamperState.TAMPER;
			default:
				throw new RSBBindingException("Could not transform " + DecimalType.Decimal.class.getName() + "! " + DecimalType.Decimal.class.getSimpleName() + "[" + decimalType + "] is unknown!");
		}
	}

	public static DecimalType.Decimal transform(TamperType.Tamper.TamperState tamperState) throws TypeNotSupportedException, RSBBindingException {
		switch (tamperState) {
			case NO_TAMPER:
				return DecimalType.Decimal.newBuilder().setValue(0).build();
			case TAMPER:
				return DecimalType.Decimal.newBuilder().setValue(1).build();
			case UNKNOWN:
				throw new TypeNotSupportedException(tamperState, DecimalType.Decimal.class);
			default:
				throw new RSBBindingException("Could not transform " + TamperType.Tamper.TamperState.class.getName() + "! " + TamperType.Tamper.TamperState.class.getSimpleName() + "[" + tamperState.name() + "] is unknown!");
		}
	}
}
