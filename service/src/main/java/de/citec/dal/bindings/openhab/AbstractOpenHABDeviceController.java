/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab;

import com.google.protobuf.GeneratedMessage;
import de.citec.dal.bindings.openhab.transform.OpenHABCommandTransformer;
import de.citec.dal.data.Location;
import de.citec.dal.exception.DALException;
import de.citec.dal.hal.AbstractDeviceController;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.VerificationFailedException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Future;
import rst.homeautomation.openhab.OpenhabCommandType;

/**
 *
 * @author Divine <DivineThreepwood@gmail.com>
 * @param <M>
 * @param <MB>
 */
public abstract class AbstractOpenHABDeviceController<M extends GeneratedMessage, MB extends GeneratedMessage.Builder> extends AbstractDeviceController<M, MB> {

//    private final static OpenhabBinding openhabBinding = OpenhabBinding.getInstance();

    public AbstractOpenHABDeviceController(String id, String label, Location location, MB builder) throws VerificationFailedException, DALException {
        super(id, label, location, builder);
    }

    public void receiveUpdate(OpenhabCommandType.OpenhabCommand command) throws CouldNotPerformException {
        logger.debug("receiveUpdate [" + command.getItem() + "=" + command.getType() + "]");

        String id_suffix = command.getItem().replaceFirst(id + "_", "");
        //TODO mpohling: Resolve mapping by service not by unit type.
        Method relatedMethod = halFunctionMapping.get(id_suffix);

        if (relatedMethod == null) {
            logger.warn("Could not apply update: Related Method unknown!");
            return;
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
}
