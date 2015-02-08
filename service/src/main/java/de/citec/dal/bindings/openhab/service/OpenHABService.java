/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab.service;

import de.citec.dal.bindings.openhab.OpenhabBinding;
import de.citec.dal.bindings.openhab.OpenhabBindingInterface;
import de.citec.dal.hal.service.Service;
import de.citec.dal.hal.device.DeviceInterface;
import de.citec.dal.hal.unit.UnitInterface;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.exception.NotSupportedException;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.openhab.OpenhabCommandType;

/**
 *
 * @author mpohling
 * @param <ST> related service type
 */
public abstract class OpenHABService<ST extends Service & UnitInterface> {

	public enum SupportedServiceType {

		BRIGHTNESS, COLOR, POWER;

		public static SupportedServiceType valueOfByServiceName(String serviceName) throws NotSupportedException {
			try {
				return valueOf(serviceName.toUpperCase());
			} catch (IllegalArgumentException ex) {
				throw new NotSupportedException(serviceName, OpenHABService.class.getSimpleName());
			}
		}
	}

	private static final OpenhabBindingInterface openhabBinding = OpenhabBinding.getInstance();

	private final Logger logger = LoggerFactory.getLogger(getClass());

	protected final DeviceInterface device;
	protected final ST unit;
	private final String itemID;

	public OpenHABService(DeviceInterface device, ST unit) {
		this.device = device;
		this.unit = unit;
		this.itemID = generateItemId();
	}

	public final String generateItemId() {
		return device.getId() + "_" + unit.getName() + "_" + getClass().getSimpleName().replaceFirst("Service", "").replaceFirst("Provider", "").replaceFirst("Impl", "");
	}

	public Future executeCommand(final OpenhabCommandType.OpenhabCommand.Builder command) throws CouldNotPerformException {
		if (itemID == null) {
			throw new NotAvailableException("itemID");
		}
		return executeCommand(itemID, command, OpenhabCommandType.OpenhabCommand.ExecutionType.SYNCHRONOUS);
	}

	public Future executeCommand(final String itemName, final OpenhabCommandType.OpenhabCommand.Builder command, final OpenhabCommandType.OpenhabCommand.ExecutionType type) throws CouldNotPerformException {
		if (command == null) {
			throw new CouldNotPerformException("Skip sending empty command!", new NullPointerException("Argument command is null!"));
		}

		if (openhabBinding == null) {
			throw new CouldNotPerformException("Skip sending command, binding not ready!", new NullPointerException("Argument rsbBinding is null!"));
		}

		logger.debug("Execute command: Setting item [" + itemID + "] to [" + command.getType().toString() + "]");
		command.setItem(itemName).setExecutionType(type);
		return openhabBinding.executeCommand(command.build());
	}
}
