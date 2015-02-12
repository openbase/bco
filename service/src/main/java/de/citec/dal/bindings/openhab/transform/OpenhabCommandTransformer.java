/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab.transform;

import de.citec.dal.bindings.openhab.OpenhabBinding;
import de.citec.dal.bindings.openhab.service.OpenHABService;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.CouldNotTransformException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.exception.NotSupportedException;
import javax.naming.OperationNotSupportedException;
import rst.homeautomation.openhab.OpenhabCommandType;

/**
 *
 * @author mpohling
 */
public final class OpenhabCommandTransformer {

	public static Object getServiceData(OpenhabCommandType.OpenhabCommand command, String serviceName) throws CouldNotPerformException {

		// Detect service type
		OpenHABService.SupportedServiceType serviceType;
		try {
			serviceType = OpenHABService.SupportedServiceType.valueOfByServiceName(serviceName);
		} catch (CouldNotPerformException ex) {
			throw new NotAvailableException("ServiceData", ex);
		}

		// Transform service data.
		switch (command.getType()) {
			case DECIMAL:
				// native double type
				return command.getDecimal();
			case HSB:
				switch (serviceType) {
					case COLOR:
						return HSVColorTransformer.transform(command.getHsb());
					default:
						throw new NotSupportedException(serviceType, OpenhabBinding.class);
				}
			case INCREASEDECREASE:
//				return command.getIncreaseDecrease();
				throw new NotSupportedException(command.getType(), OpenhabCommandTransformer.class);
			case ONOFF:
				return PowerStateTransformer.transform(command.getOnOff().getState());
			case OPENCLOSED:
				return OpenClosedStateTransformer.transform(command.getOpenClosed().getState());
			case PERCENT:
				// native int type
				return command.getPercent().getValue();
			case STOPMOVE:
				return StopMoveStateTransformer.transform(command.getStopMove().getState());
			case STRING:
				// native string type
				return command.getText();
			case UPDOWN:
				return UpDownStateTransformer.transform(command.getUpDown().getState());
			default:
				throw new CouldNotTransformException("No corresponding data found for " + command + ".");
		}
	}
}
