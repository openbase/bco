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
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
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

    private String ITEM_ID_DELIMITER = "_";
    private final static ServiceFactory defaultServiceFactory = new OpenhabServiceFactory();
    
//    private final static OpenhabBinding openhabBinding = OpenhabBinding.getInstance();

    public AbstractOpenHABDeviceController(String id, String label, Location location, MB builder) throws InstantiationException {
        super(id, label, location, builder);
    }

    public void receiveUpdate(OpenhabCommandType.OpenhabCommand command) throws CouldNotPerformException {
        logger.debug("receiveUpdate [" + command.getItem() + "=" + command.getType() + "]");

        String unitServicePattern = command.getItem().replaceFirst(id + "_", "");
        String[] pattern = unitServicePattern.split(ITEM_ID_DELIMITER);
        String unitName = pattern[0];
        String serviceName = pattern[1];
        
        
        //TODO mpohling: Resolve mapping by service not by unit type.
        Method relatedMethod = halFunctionMapping.get(unitName);

        if (relatedMethod == null) {
            throw new CouldNotPerformException("Could not apply update: Related Method["+id_suffix+"] unknown!");
        }

        try {
            relatedMethod.invoke(this, OpenHABCommandTransformer.getCommandData(command));
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
    
    

//    public Future executeCommand(final String itemName, final OpenhabCommandType.OpenhabCommand.Builder commandBuilder, final OpenhabCommandType.OpenhabCommand.ExecutionType type) throws CouldNotPerformException {
//        commandBuilder.setItem(itemName).setExecutionType(type);
//        return openhabBinding.executeCommand(commandBuilder.build());
//    }

    @Override
    public ServiceFactory getDefaultServiceFactory() {
        return defaultServiceFactory;
    }
}
