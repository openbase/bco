/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab;

import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.hal.AbstractDeviceController;
import de.citec.dal.hal.service.Service;
import de.citec.dal.hal.unit.DeviceInterface;
import de.citec.jul.exception.CouldNotPerformException;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.openhab.OpenhabCommandType;

/**
 *
 * @author mpohling
 * @param <ST> related service type
 */
public class OpenHABServiceImpl<ST extends Service> {
    
    private static final OpenhabBindingInterface openhabBinding = OpenhabBinding.getInstance();
    
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    protected final DeviceInterface device;
    protected final ST unit;
    private final String deviceId;

    public OpenHABServiceImpl(DeviceInterface device, ST unit) {
        this.device = device;
        this.unit = unit;
        this.deviceId = generateHardwareId();
    }
    
    public final String generateHardwareId() {
        return device.getId() + "_" + unit;
    }

    public Future executeCommand(final OpenhabCommandType.OpenhabCommand.Builder command) throws CouldNotPerformException {
        if (deviceId == null) {
            throw new CouldNotPerformException("Skip sending command, could not generate id!", new NullPointerException("Argument id is null!"));
        }
        return executeCommand(deviceId, command, OpenhabCommandType.OpenhabCommand.ExecutionType.SYNCHRONOUS);
    }

    public Future executeCommand(final String itemName, final OpenhabCommandType.OpenhabCommand.Builder command, final OpenhabCommandType.OpenhabCommand.ExecutionType type) throws CouldNotPerformException {
        if (command == null) {
            throw new CouldNotPerformException("Skip sending empty command!", new NullPointerException("Argument command is null!"));
        }

        if (openhabBinding == null) {
            throw new CouldNotPerformException("Skip sending command, binding not ready!", new NullPointerException("Argument rsbBinding is null!"));
        }

        logger.debug("Execute command: Setting item [" + deviceId + "] to [" + command.getType().toString() + "]");
        command.setItem(itemName).setExecutionType(type);
        return openhabBinding.executeCommand(command.build());
    }
}
