/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.service;

import de.citec.dal.bindings.openhab.service.OpenHABService;
import de.citec.dal.hal.provider.BatteryProvider;
import de.citec.dal.hal.provider.ButtonProvider;
import de.citec.dal.hal.provider.HandleProvider;
import de.citec.dal.hal.provider.MotionProvider;
import de.citec.dal.hal.provider.TamperProvider;
import de.citec.dal.hal.provider.TemperatureProvider;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.exception.NotSupportedException;
import de.citec.jul.extension.rsb.iface.RSBLocalServerInterface;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.RSBException;
import rsb.patterns.Callback;
import rsb.patterns.LocalServer;

/**
 *
 * @author mpohling
 */
public enum ServiceType {

	MULTI(MultiService.class),
	BATTERY(BatteryProvider.class),
	BRIGHTNESS(BrightnessService.class),
	BUTTON(ButtonProvider.class),
	COLOR(ColorService.class),
	HANDLE(HandleProvider.class),
	TAMPER(TamperProvider.class),
	TEMPERATURE(TemperatureProvider.class),
	POWER(PowerService.class),
	SHUTTER(ShutterService.class),
	OPENING_RATIO(OpeningRatioService.class),
	MOTION(MotionProvider.class),
        DIMM(DimmService.class);

	public static final String SET = "set";
	public static final String UPDATE = "update";
	public static final String PROVIDER = "Provider";
	public static final String SERVICE = "Service";

	private static final Logger logger = LoggerFactory.getLogger(ServiceType.class);

	private final Class<? extends Service> serviceClass;
	private final Method[] methodDeclarations;

	private ServiceType(final Class<? extends Service> serviceClass) {
		this.serviceClass = serviceClass;
		this.methodDeclarations = serviceClass.getDeclaredMethods();
	}

	public Class<? extends Service> getServiceClass() {
		return serviceClass;
	}

	public List<String> getUpdateMethods() {
		final List<String> updateMethodList = new ArrayList<>();
		for (Method method : getDeclaredMethods()) {
			if (!method.getName().startsWith(SET)) {
				continue;
			}
			updateMethodList.add(UPDATE + method.getName().replaceFirst(SET, ""));
		}
		return updateMethodList;
	}

	public Method[] getDeclaredMethods() {
		return methodDeclarations;
	}

	public String getServiceName() {
		return getServiceClass().getSimpleName().replace(PROVIDER, "").replace(SERVICE, "");
	}

	public static List<ServiceType> getServiceTypeList(final Service service) {
		List<ServiceType> serviceTypeList = new ArrayList<>();
		for (ServiceType serviceType : values()) {
			if (serviceType.getServiceClass().isAssignableFrom(service.getClass())) {
				serviceTypeList.add(serviceType);
			}
		}
		return serviceTypeList;
	}

	public static List<Class<? extends Service>> getServiceClassList(final Service service) {
		List<Class<? extends Service>> serviceClassList = new ArrayList<>();
		for (ServiceType serviceType : getServiceTypeList(service)) {
			serviceClassList.add(serviceType.getServiceClass());
		}
		return serviceClassList;
	}

	public static void registerServiceMethods(final RSBLocalServerInterface server, final Service service) {
		for (ServiceType serviceType : ServiceType.getServiceTypeList(service)) {
			for (Method method : serviceType.getDeclaredMethods()) {
				try {
					server.addMethod(method.getName(), getCallback(method, service, serviceType));
				} catch (CouldNotPerformException ex) {
					logger.warn("Could not register callback for service methode " + method.toGenericString(), ex);
				}
			}
		}
	}

	private static Callback getCallback(final Method method, final Service service, final ServiceType serviceType) throws CouldNotPerformException {
		String callbackName = method.getName().concat(Callback.class.getSimpleName());
		try {
			for (Class callbackClass : serviceType.getServiceClass().getDeclaredClasses()) {
				if (callbackClass.getSimpleName().equalsIgnoreCase(callbackName)) {
					return (Callback) callbackClass.getConstructor(serviceType.getServiceClass()).newInstance(service);
				}
			}
		} catch (Exception ex) {
			throw new NotAvailableException(callbackName);
		}
		throw new NotSupportedException(callbackName, service);
	}

	public static ServiceType valueOfByServiceName(String serviceName) throws NotSupportedException {
		try {
			for (ServiceType serviceType : ServiceType.values()) {
				if (serviceType.getServiceName().equalsIgnoreCase(serviceName)) {
					return serviceType;
				}
			}
			throw new NotAvailableException(serviceName);
		} catch (IllegalArgumentException | CouldNotPerformException ex) {
			throw new NotSupportedException(serviceName, OpenHABService.class.getSimpleName());
		}
	}
}
