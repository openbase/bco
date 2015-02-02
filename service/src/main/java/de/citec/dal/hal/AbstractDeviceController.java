/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal;

import com.google.protobuf.GeneratedMessage;
import de.citec.dal.bindings.openhab.OpenhabBinding;
import de.citec.dal.bindings.openhab.OpenhabBindingInterface;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import de.citec.dal.data.Location;
import de.citec.dal.exception.DALException;
import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.hal.unit.HardwareUnit;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.VerificationFailedException;
import de.citec.jul.rsb.RSBCommunicationService;
import de.citec.jul.rsb.RSBInformerInterface.InformerType;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Future;
import rsb.RSBException;
import rsb.patterns.LocalServer;
import rst.homeautomation.openhab.OpenhabCommandType;
import rst.homeautomation.openhab.OpenhabCommandType.OpenhabCommand;
import rst.homeautomation.openhab.OpenhabCommandType.OpenhabCommand.ExecutionType;

/**
 *
 * @author Divine <DivineThreepwood@gmail.com>
 * @param <M> Underling message type.
 * @param <MB> Message related builder.
 */
public abstract class AbstractDeviceController<M extends GeneratedMessage, MB extends GeneratedMessage.Builder> extends RSBCommunicationService<M, MB> implements HardwareUnit {

	public final static String ID_SEPERATOR = "_";
	public final static String TYPE_FILED_ID = "id";
	public final static String TYPE_FILED_LABEL= "label";

	protected final String id;
	protected final String label;
	protected final String hardware_id;
	protected final String instance_id;
	protected final Location location;
	protected final Map<String, Method> halFunctionMapping;
	protected final Map<String, AbstractUnitController> unitMap;

	protected OpenhabBindingInterface rsbBinding = OpenhabBinding.getInstance();

	public AbstractDeviceController(final String id, final String label, final Location location, final MB builder) throws VerificationFailedException, DALException {
		super(generateScope(id, location), builder);
		this.id = id;
		this.label = label;
		this.hardware_id = parseHardwareId(id, getClass());
		this.instance_id = parseInstanceId(id);
		this.location = location;
		this.unitMap = new HashMap<>();
		this.halFunctionMapping = new HashMap<>();

		setField(TYPE_FILED_ID, id);
		setField(TYPE_FILED_LABEL, label);

		try {
			init(InformerType.Distributed);
		} catch (RSBException ex) {
			throw new DALException("Could not init RSBCommunicationService!", ex);
		}

		try {
			initHardwareMapping();
		} catch (Exception ex) {
			throw new DALException("Could not apply hardware mapping for " + getClass().getSimpleName() + "!", ex);
		}
	}

	public final static String parseHardwareId(String id, Class<? extends AbstractDeviceController> hardware) throws VerificationFailedException {
		assert id != null;
		assert hardware != null;
		String hardwareId = hardware.getSimpleName().replace("Controller", "");

		/* verify given id */
		if (!id.startsWith(hardwareId)) {
			throw new VerificationFailedException("Given id [" + id + "] does not start with prefix [" + hardwareId + "]!");
		}

		return hardwareId;
	}

	public final static String parseInstanceId(String id) throws VerificationFailedException {
		String[] split = id.split(ID_SEPERATOR);
		String instanceId;

		try {
			instanceId = split[split.length - 1];
		} catch (IndexOutOfBoundsException ex) {
			throw new VerificationFailedException("Given id [" + id + "] does not contain saperator [" + ID_SEPERATOR + "]");
		}

		/* verify instance id */
		try {
			Integer.parseInt(instanceId);
		} catch (NumberFormatException ex) {
			throw new VerificationFailedException("Given id [" + id + "] does not end with a instance nubmer!");
		}
		return instanceId;
	}

	protected <U extends AbstractUnitController> void register(final U hardware) {
		unitMap.put(hardware.getId(), hardware);
	}

	@Override
	public Location getLocation() {
		return location;
	}

	//TODO tamino: Make AbstractDeviceController openhab independent. Move all type transformations in dal-openhab-binding.
	public void internalReceiveUpdate(String itemName, OpenhabCommand command) throws CouldNotPerformException {
		logger.debug("internalReceiveUpdate [" + itemName + "=" + command.getType() + "]");

		String id_suffix = itemName.replaceFirst(id + "_", "");
		//TODO mpohling: Resolve mapping by service not by unit type.
		Method relatedMethod = halFunctionMapping.get(id_suffix);

		if (relatedMethod == null) {
			logger.warn("Could not apply update: Related Method unknown!");
			return;
		}

		try {
			switch (command.getType()) {
				case DECIMAL:
					relatedMethod.invoke(this, command.getDecimal());
					break;
				case HSB:
					relatedMethod.invoke(this, command.getHsb());
					break;
				case INCREASEDECREASE:
					relatedMethod.invoke(this, command.getIncreaseDecrease());
					break;
				case ONOFF:
					relatedMethod.invoke(this, command.getOnOff().getState());
					break;
				case OPENCLOSED:
					relatedMethod.invoke(this, command.getOpenClosed().getState());
					break;
				case PERCENT:
					relatedMethod.invoke(this, command.getPercent().getValue());
					break;
				case STOPMOVE:
					relatedMethod.invoke(this, command.getStopMove().getState());
					break;
				case STRING:
					relatedMethod.invoke(this, command.getText());
					break;
				case UPDOWN:
					relatedMethod.invoke(this, command.getUpDown().getState());
					break;
				default:
					logger.warn("No corresponding Openhab command type found. Skip message invocation.");
					break;
			}
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
	public String getId() {
		return id;
	}

	public String getLable() {
		return label;
	}

	@Override
	public void activate() {
		super.activate();

		for (AbstractUnitController controller : unitMap.values()) {
			controller.activate();
		}
	}

	@Override
	public void deactivate() throws InterruptedException {
		super.deactivate();

		for (AbstractUnitController controller : unitMap.values()) {
			controller.deactivate();
		}
	}

	@Override
	public void registerMethods(LocalServer server) {
		// dummy construct: For registering methods overwrite this method.
	}

	public Future executeCommand(final String itemName, final OpenhabCommandType.OpenhabCommand.Builder commandBuilder, final ExecutionType type) throws RSBBindingException {
		commandBuilder.setItem(itemName).setExecutionType(type);
		return rsbBinding.executeCommand(commandBuilder.build());
	}

	@Override
	public String getHardware_id() {
		return hardware_id;
	}

	@Override
	public String getInstance_id() {
		return instance_id;
	}

	protected abstract void initHardwareMapping() throws NoSuchMethodException, SecurityException;

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[id:" + id + "|scope:" + getLocation().getScope() + "]";
	}

	public Collection<AbstractUnitController> getUnits() {
		return Collections.unmodifiableCollection(unitMap.values());
	}
}

