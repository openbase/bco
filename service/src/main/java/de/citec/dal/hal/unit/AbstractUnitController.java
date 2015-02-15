/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.unit;

import de.citec.dal.hal.device.DeviceInterface;
import com.google.protobuf.GeneratedMessage;
import de.citec.dal.data.Location;
import de.citec.dal.hal.service.Service;
import de.citec.dal.hal.service.ServiceType;
import de.citec.jps.exception.ValidationException;
import de.citec.jul.rsb.RSBCommunicationService;
import de.citec.jul.rsb.RSBInformerInterface;
import de.citec.jul.rsb.ScopeProvider;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.MultiException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import rsb.RSBException;
import rsb.Scope;
import rsb.patterns.LocalServer;

/**
 *
 * @author mpohling
 * @param <M> Underling message type.
 * @param <MB> Message related builder.
 */
public abstract class AbstractUnitController<M extends GeneratedMessage, MB extends GeneratedMessage.Builder> extends RSBCommunicationService<M, MB> implements UnitInterface {

	public final static String TYPE_FILED_ID = "id";
	public final static String TYPE_FILED_LABEL = "label";

	protected final String name;
	protected final String label;
	private final DeviceInterface device;
	private List<Service> serviceList;

	public AbstractUnitController(final Class unitClass, final String label, final DeviceInterface device, final MB builder) throws InstantiationException {
		super(generateScope(generateName(unitClass), label, device), builder);
		this.name = generateName();
		this.label = label;
		this.device = device;
		this.serviceList = new ArrayList<>();

		setField(TYPE_FILED_ID, name); //TODO Tamino: Fix RST Types.
		setField(TYPE_FILED_LABEL, label);

		try {
			init(RSBInformerInterface.InformerType.Distributed);
		} catch (RSBException ex) {
			throw new InstantiationException("Could not init RSBCommunicationService!", ex);
		}
		try {
			validateUpdateServices();
		} catch (MultiException ex) {
			logger.error(this + " is not valid!", ex);
			ex.printExceptionStack();
		}
	}

	@Override
	public String getName() {
		return name;
	}

	public String getLable() {
		return label;
	}

	public DeviceInterface getDevice() {
		return device;
	}

	public Collection<Service> getServices() {
		return Collections.unmodifiableList(serviceList);
	}

	public void registerService(final Service service) {
		serviceList.add(service);
	}

	public final String generateName() {
		return generateName(getClass());
	}

	public static final String generateName(Class clazz) {
		return clazz.getSimpleName().replace("Controller", "");
	}

	public Scope generateScope() {
		return generateScope(generateName(), label, device);
	}

	public static Scope generateScope(final String name, final String label, final DeviceInterface device) {
		return generateScope(name, label, device.getLocation());
	}

	public static Scope generateScope(final String name, final String label, final Location location) {
		return location.getScope().concat(new Scope(ScopeProvider.SEPARATOR + name).concat(new Scope(ScopeProvider.SEPARATOR + label)));
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + name + "[" + label + "]]";
	}

	@Override
	public void registerMethods(LocalServer server) throws RSBException {
		ServiceType.registerServiceMethods(server, this);
	}

	/***
	 *
	 * @throws MultiException
	 */
	private void validateUpdateServices() throws MultiException {
		logger.debug("Validating unit update methods...");
		
		final Map<Object, Exception> exceptions = new HashMap<>();
		List<String> unitMethods = new ArrayList<>();


		// === Transform unit methods into string list. ===
		for (Method medhod : getClass().getMethods()) {
			unitMethods.add(medhod.getName());
		}

		// === Validate if all update methods are registrated. ===
		for (ServiceType service : ServiceType.getServiceTypeList(this)) {
			for (String serviceMethod : service.getUpdateMethods()) {
				if(!unitMethods.contains(serviceMethod)) {
					exceptions.put(this, new ValidationException(this + " dose not contain service method [" + service.getUpdateMethods() + "]"));
				}
			}
		}

		// === throw multi exception in error case. ===
		if (!exceptions.isEmpty()) {
			throw new MultiException("Update service not valid!", exceptions);
		}
	}
}
