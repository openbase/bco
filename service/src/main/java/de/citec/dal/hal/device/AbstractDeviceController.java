/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.device;

import com.google.protobuf.GeneratedMessage;
import java.util.HashMap;
import java.util.Map;
import de.citec.dal.data.Location;
import de.citec.dal.hal.unit.AbstractUnitController;
import de.citec.jul.exception.CouldNotTransformException;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.rsb.com.RSBCommunicationService;
import de.citec.jul.rsb.com.RSBInformerInterface.InformerType;
import java.util.Collection;
import java.util.Collections;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.exception.VerificationFailedException;
import rsb.patterns.LocalServer;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;

/**
 *
 * @author Divine Threepwood
 * @param <M> Underling message type.
 * @param <MB> Message related builder.
 */
public abstract class AbstractDeviceController<M extends GeneratedMessage, MB extends GeneratedMessage.Builder> extends RSBCommunicationService<M, MB> implements Device {

//	public final static String TYPE_FILED_ID = "id";
//	public final static String TYPE_FILED_NAME = "name";
//	public final static String TYPE_FILED_LABEL = "label";
	public final static String DEVICE_TYPE_FILED_CONFIG = "config";

	protected final String id;
	protected final String name;
	protected final String label;
	protected final Location location;

	private final Map<String, AbstractUnitController> unitMap;

	public AbstractDeviceController(final DeviceConfig config, final MB builder) throws InstantiationException, CouldNotTransformException {
		super(config.getScope(), builder);
		this.id = config.getId();
		this.name = generateName(builder.getClass().getDeclaringClass());
		this.label = config.getLabel();
		this.location = new Location(config.getPlacementConfig().getLocation());
		this.unitMap = new HashMap<>();

        setField(DEVICE_TYPE_FILED_CONFIG, config);
//		setField(TYPE_FILED_ID, id);
//		setField(TYPE_FILED_NAME, name);
//		setField(TYPE_FILED_LABEL, label);

		try {
			init(InformerType.Distributed);
		} catch (InitializationException ex) {
			throw new InstantiationException("Could not init RSBCommunicationService!", ex);
		}
	}

//	public final static String generateId(final Scope scope) {
//		return scope.toString();
//	}
//
    
	public final static String generateName(final Class hardware) {
		return hardware.getSimpleName().replace("Controller", "");
	}

	protected <U extends AbstractUnitController> void registerUnit(final U unit) throws VerificationFailedException {
		if(unitMap.containsKey(unit.getId())) {
			throw new VerificationFailedException("Could not register "+unit+"! Unit with same name already registered!");
		}
		unitMap.put(unit.getId(), unit);
	}

	public AbstractUnitController getUnit(final String id) throws NotAvailableException {
		if (!unitMap.containsKey(id)) {
			throw new NotAvailableException("Unit[" + id + "]", this + " has no registered unit with given name!");
		}
		return unitMap.get(name);
	}

	@Override
	public Location getLocation() {
		return location;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public void activate() {
		super.activate();

		for (AbstractUnitController unit : unitMap.values()) {
			unit.activate();
		}
	}

	@Override
	public void deactivate() throws InterruptedException {
		super.deactivate();

		for (AbstractUnitController unit : unitMap.values()) {
			unit.deactivate();
		}
	}

	@Override
	public void registerMethods(LocalServer server) {
		// dummy construct: For registering methods overwrite this method.
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["+id+"]";
	}

	public Collection<AbstractUnitController> getUnits() {
		return Collections.unmodifiableCollection(unitMap.values());
	}
}
