/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab;

import static de.citec.dal.bindings.openhab.transform.ItemTransformer.ITEM_SEGMENT_DELIMITER;
import static de.citec.dal.bindings.openhab.transform.ItemTransformer.ITEM_SUBSEGMENT_DELIMITER;
import de.citec.dal.bindings.openhab.transform.OpenhabCommandTransformer;
import de.citec.dal.hal.service.ServiceType;
import de.citec.dal.hal.unit.Unit;
import de.citec.dal.registry.UnitRegistry;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.NotAvailableException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.Scope;
import rst.homeautomation.openhab.OpenhabCommandType;

/**
 *
 * @author Divine Threepwood
 */
public class OpenHABCommandExecutor {

	private static final Logger logger = LoggerFactory.getLogger(OpenHABCommandExecutor.class);

	private final UnitRegistry unitRegistry;

	public OpenHABCommandExecutor(UnitRegistry unitRegistry) {
		this.unitRegistry = unitRegistry;
	}

	public void receiveUpdate(OpenhabCommandType.OpenhabCommand command) throws CouldNotPerformException {
		logger.info("receiveUpdate [" + command.getItem() + "=" + command.getType() + "]");

		String[] nameSegment = command.getItem().split(ITEM_SEGMENT_DELIMITER);
		String location = nameSegment[1].replace(ITEM_SUBSEGMENT_DELIMITER, Scope.COMPONENT_SEPARATOR);
		String unitId = (Scope.COMPONENT_SEPARATOR + location + Scope.COMPONENT_SEPARATOR + nameSegment[2] + Scope.COMPONENT_SEPARATOR + nameSegment[3] + Scope.COMPONENT_SEPARATOR).toLowerCase();
		String serviceName = nameSegment[4];
		Object serviceData = OpenhabCommandTransformer.getServiceData(command, serviceName);
		Method relatedMethod;
		Unit unit;

		try {
			unit = unitRegistry.get(unitId);

			String methodName = ServiceType.UPDATE + serviceName;
			try {
				relatedMethod = unit.getClass().getMethod(methodName, serviceData.getClass());
				if (relatedMethod == null) {
					throw new NotAvailableException(relatedMethod);
				}
			} catch (NoSuchMethodException | SecurityException | NotAvailableException ex) {
				throw new NotAvailableException("Method " + unit + "." + methodName + "(" + serviceData.getClass() + ")", ex);
			}
		} catch (CouldNotPerformException ex) {
			throw new CouldNotPerformException("Unit not compatible!", ex);
		}

		try {
			relatedMethod.invoke(unit, serviceData);
		} catch (IllegalAccessException ex) {
			throw new CouldNotPerformException("Cannot access related Method [" + relatedMethod.getName() + "]", ex);
		} catch (IllegalArgumentException ex) {
			throw new CouldNotPerformException("Does not match [" + relatedMethod.getParameterTypes()[0].getName() + "] which is needed by [" + relatedMethod.getName() + "]!", ex);
		} catch (InvocationTargetException ex) {
			throw new CouldNotPerformException("The related method [" + relatedMethod.getName() + "] throws an exceptioin during invocation!", ex);
		} catch (Throwable cause) {
			throw new CouldNotPerformException("Fatal invocation error!", cause);
		}
	}
}
