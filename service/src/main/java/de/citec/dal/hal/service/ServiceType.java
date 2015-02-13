/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.service;

import de.citec.dal.bindings.openhab.service.OpenHABService;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.exception.NotSupportedException;
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

	BRIGHTNESS(BrightnessService.class),
	COLOR(ColorService.class),
	POWER(PowerService.class),
    SHUTTER(ShutterService.class),
    OPENINGRATIO(OpeningRatioService.class);

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

	public Method[] getDeclaredMethods() {
		return methodDeclarations;
	}

	public static List<ServiceType> getServiceTypeList(final Service service) {
		List<ServiceType> serviceTypeList = new ArrayList<>();
		for (ServiceType serviceType : values()) {
			if (service.getClass().isAssignableFrom(serviceType.getServiceClass())) {
				serviceTypeList.add(serviceType);
			}
		}
		return serviceTypeList;
	}

	public static List<Class<? extends Service>> getServiceClassList(final Service service) {
		List<Class<? extends Service>> serviceClassList = new ArrayList<>();
		for (ServiceType serviceType : getServiceTypeList(service)) {
			serviceClassList.add(serviceType.serviceClass);
		}
		return serviceClassList;
	}

	public static void registerServiceMethods(final LocalServer server, final Service service) {
		for (ServiceType serviceType : ServiceType.getServiceTypeList(service)) {
			for (Method method : serviceType.getDeclaredMethods()) {
				try {
					server.addMethod(method.getName(), getCallback(method, service, serviceType));
				} catch (RSBException | CouldNotPerformException ex) {
					logger.warn("Could not register callback for service methode " + method.toGenericString(), ex);
				}
			}
		}
	}

	private static Callback getCallback(final Method method, final Service service, final ServiceType serviceType) throws CouldNotPerformException {
		String callbackName = method.getName().concat(Callback.class.getSimpleName());
		try {
			for (Class callbackClass : serviceType.getClass().getDeclaredClasses()) {
				if (callbackClass.getSimpleName().equalsIgnoreCase(callbackName)) {
					return (Callback) callbackClass.getConstructor(callbackClass, serviceType.getClass()).newInstance(service);
				}
			}
		} catch (Exception ex) {
			throw new NotAvailableException(callbackName);
		}
		throw new NotSupportedException(callbackName, service);
	}

	public static ServiceType valueOfByServiceName(String serviceName) throws NotSupportedException {
		try {
			return valueOf(serviceName.toUpperCase());
		} catch (IllegalArgumentException ex) {
			throw new NotSupportedException(serviceName, OpenHABService.class.getSimpleName());
		}
	}
}
