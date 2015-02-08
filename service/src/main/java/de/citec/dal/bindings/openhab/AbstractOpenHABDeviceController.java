/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab;

import com.google.protobuf.GeneratedMessage;
import de.citec.dal.bindings.openhab.service.OpenhabServiceFactory;
import de.citec.dal.bindings.openhab.transform.OpenHABCommandTransformer;
import de.citec.dal.data.Location;
import de.citec.dal.hal.device.AbstractDeviceController;
import de.citec.dal.hal.service.ServiceFactory;
import de.citec.dal.hal.unit.AbstractUnitController;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.NotAvailableException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import rst.homeautomation.openhab.OpenhabCommandType;

/**
 *
 * @author Divine <DivineThreepwood@gmail.com>
 * @param <M>
 * @param <MB>
 */
public abstract class AbstractOpenHABDeviceController<M extends GeneratedMessage, MB extends GeneratedMessage.Builder> extends AbstractDeviceController<M, MB> {

    private static final String ITEM_ID_DELIMITER = "_";
    private final static ServiceFactory defaultServiceFactory = new OpenhabServiceFactory();

    public AbstractOpenHABDeviceController(String id, String label, Location location, MB builder) throws InstantiationException {
        super(id, label, location, builder);
    }

    public void receiveUpdate(OpenhabCommandType.OpenhabCommand command) throws CouldNotPerformException {
        logger.debug("receiveUpdate [" + command.getItem() + "=" + command.getType() + "]");

        String unitServicePattern = command.getItem().replaceFirst(id + "_", "");
        String[] pattern = unitServicePattern.split(ITEM_ID_DELIMITER);
        String unitName = pattern[0];
        String serviceName = pattern[1];

        Object serviceData = OpenHABCommandTransformer.getServiceData(command, serviceName);
        AbstractUnitController unit;
        Method relatedMethod;

        try {
            unit = getUnitByName(unitName);
            
            String methodName = "set" + serviceName;
            try {
                relatedMethod = unit.getClass().getMethod(methodName, serviceData.getClass());
                if (relatedMethod == null) {
                    throw new NotAvailableException(relatedMethod);
                }
            } catch (Exception ex) {
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
        } catch (Exception ex) {
            throw new CouldNotPerformException("Fatal invocation error!", ex);
        }
    }

    @Override
    public ServiceFactory getDefaultServiceFactory() {
        return defaultServiceFactory;
    }
}
