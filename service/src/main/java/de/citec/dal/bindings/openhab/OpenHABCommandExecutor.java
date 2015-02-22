/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab;

import static de.citec.dal.bindings.openhab.AbstractOpenHABDeviceController.ITEM_ID_DELIMITER;
import de.citec.dal.bindings.openhab.transform.ItemTransformer;
import de.citec.dal.bindings.openhab.transform.OpenhabCommandTransformer;
import de.citec.dal.hal.service.Service;
import de.citec.dal.hal.service.ServiceType;
import de.citec.dal.hal.unit.AbstractUnitController;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.NotAvailableException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.openhab.OpenhabCommandType;

/**
 *
 * @author Divine <DivineThreepwood@gmail.com>
 */
public class OpenHABCommandExecutor {

	private static final Logger logger = LoggerFactory.getLogger(OpenHABCommandExecutor.class);

	public void receiveUpdate(OpenhabCommandType.OpenhabCommand command, Unit) throws CouldNotPerformException {
        logger.debug("receiveUpdate [" + command.getItem() + "=" + command.getType() + "]");

        String unitServicePattern = command.getItem().replaceFirst(name + ITEM_ID_DELIMITER, "");
        String[] pattern = unitServicePattern.split(ITEM_ID_DELIMITER);
        String unitName = pattern[0];
        String serviceName = pattern[pattern.length-1];

        Object serviceData = OpenhabCommandTransformer.getServiceData(command, serviceName);
        AbstractUnitController unit;
        Method relatedMethod;

        try {
            unit = getUnitByName(unitName);

            String methodName = ServiceType.UPDATE + serviceName;
            try {
                relatedMethod = unit.getClass().getMethod(methodName, serviceData.getClass());
                if (relatedMethod == null) {
                    throw new NotAvailableException(relatedMethod);
                }
            } catch (NoSuchMethodException | SecurityException | NotAvailableException ex) {
                throw new NotAvailableException("Methcode " + unit + "." + methodName + "(" + serviceData.getClass() + ")", ex);
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
