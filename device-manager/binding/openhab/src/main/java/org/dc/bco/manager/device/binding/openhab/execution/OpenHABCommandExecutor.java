/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.device.binding.openhab.execution;

import org.dc.bco.manager.device.binding.openhab.transform.OpenhabCommandTransformer;
import org.dc.bco.dal.lib.layer.service.ServiceType;
import org.dc.bco.dal.lib.layer.unit.Unit;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.processing.StringProcessor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.dc.bco.dal.lib.layer.unit.UnitControllerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.Scope;
import rst.homeautomation.openhab.OpenhabCommandType;
import rst.homeautomation.openhab.OpenhabCommandType.OpenhabCommand;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate;

/**
 *
 * @author Divine Threepwood
 */
public class OpenHABCommandExecutor {
    
    public static final String ITEM_SUBSEGMENT_DELIMITER = "_";
    public static final String ITEM_SEGMENT_DELIMITER = "__";
    
    private static final Logger logger = LoggerFactory.getLogger(OpenHABCommandExecutor.class);
    
    private final UnitControllerRegistry unitRegistry;
    
    public OpenHABCommandExecutor(UnitControllerRegistry unitRegistry) {
        this.unitRegistry = unitRegistry;
    }
    
    private class OpenhabCommandMetaData {
        
        private final OpenhabCommandType.OpenhabCommand command;
        private final ServiceTemplate.ServiceType serviceType;
        private final String unitId;
        private final String locationId;
        
        public OpenhabCommandMetaData(OpenhabCommand command) throws CouldNotPerformException {
            this.command = command;
            
            try {
                String[] nameSegment = command.getItem().split(ITEM_SEGMENT_DELIMITER);
                try {
                    locationId = nameSegment[1].replace(ITEM_SUBSEGMENT_DELIMITER, Scope.COMPONENT_SEPARATOR);
                } catch (Exception ex) {
                    throw new CouldNotPerformException("Could not extract location id out of item name!");
                }
                try {
                    this.unitId = (Scope.COMPONENT_SEPARATOR + locationId + Scope.COMPONENT_SEPARATOR + nameSegment[2] + Scope.COMPONENT_SEPARATOR + nameSegment[3] + Scope.COMPONENT_SEPARATOR).toLowerCase();
                } catch (Exception ex) {
                    throw new CouldNotPerformException("Could not extract unit id out of item name!");
                }
                try {
                    serviceType = ServiceTemplate.ServiceType.valueOf(StringProcessor.transformToUpperCase(nameSegment[4]));
                } catch (Exception ex) {
                    throw new CouldNotPerformException("Could not extract service type out of item name!", ex);
                }
            } catch (Exception ex) {
                throw new CouldNotPerformException("Could not extract meta data out of openhab command because Item[" + command.getItem() + "] not compatible!", ex);
            }
        }
        
        public OpenhabCommand getCommand() {
            return command;
        }
        
        public String getServiceTypeName() {
            return StringProcessor.transformUpperCaseToCamelCase(serviceType.name()).replaceAll("Provider", "").replaceAll("Service", "");
        }
        
        public ServiceTemplate.ServiceType getServiceType() {
            return serviceType;
        }
        
        public String getUnitId() {
            return unitId;
        }
        
        public String getLocationId() {
            return locationId;
        }
    }
    
    public void receiveUpdate(OpenhabCommandType.OpenhabCommand command) throws CouldNotPerformException {
        logger.info("receiveUpdate [" + command.getItem() + "=" + command.getType() + "]");
        
        OpenhabCommandMetaData metaData = new OpenhabCommandMetaData(command);
        
        Object serviceData = OpenhabCommandTransformer.getServiceData(command, metaData.getServiceType());
        Method relatedMethod;
        Unit unit;
        
        try {
            unit = unitRegistry.get(metaData.getUnitId());
            
            String updateMethodName = ServiceType.UPDATE + metaData.getServiceTypeName();
            try {
                relatedMethod = unit.getClass().getMethod(updateMethodName, serviceData.getClass());
                if (relatedMethod == null) {
                    throw new NotAvailableException(relatedMethod);
                }
            } catch (NoSuchMethodException | SecurityException | NotAvailableException ex) {
                throw new NotAvailableException("Method " + unit + "." + updateMethodName + "(" + serviceData.getClass() + ")", ex);
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
