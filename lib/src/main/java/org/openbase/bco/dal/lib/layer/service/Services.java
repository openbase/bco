package org.openbase.bco.dal.lib.layer.service;

/*
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.Type;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.ProtocolMessageEnum;
import org.openbase.bco.dal.lib.layer.service.consumer.ConsumerService;
import org.openbase.bco.dal.lib.layer.service.operation.OperationService;
import org.openbase.bco.dal.lib.layer.service.provider.ProviderService;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.protobuf.processing.ProtoBufFieldProcessor;
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.service.ServiceCommunicationTypeType.ServiceCommunicationType.CommunicationType;
import org.openbase.type.domotic.service.ServiceDescriptionType.ServiceDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.service.ServiceTempusTypeType.ServiceTempusType.ServiceTempus;
import org.openbase.type.domotic.state.ColorStateType.ColorState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.openbase.bco.dal.lib.layer.service.Service.SERVICE_STATE_PACKAGE;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 * @author <a href="mailto:agatting@techfak.uni-bielefeld.de">Andreas Gatting</a>
 */
public class Services extends ServiceStateProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(Services.class);

    /**
     * This method returns the service base name of the given service type.
     * <p>
     * The base name is the service name without service suffix.
     * e.g. the base name of service PowerStateService is PowerState.
     *
     * @param serviceType the service type to extract the base name.
     *
     * @return the service base name.
     */
    public static String getServiceBaseName(ServiceType serviceType) {
        return StringProcessor.transformUpperCaseToPascalCase(serviceType.name()).replaceAll(Service.SERVICE_LABEL, "");
    }

    public static String getServiceMethodPrefix(final ServicePattern pattern) throws CouldNotPerformException {
        switch (pattern) {
            case CONSUMER:
                return "";
            case OPERATION:
                return "set";
            case PROVIDER:
                return "get";
            default:
                throw new NotSupportedException(pattern, Services.class);
        }
    }

    /**
     * Method returns the state name of the appurtenant service.
     *
     * @param serviceType the service type which is used to generate the service name.
     *
     * @return The state type name as string.
     *
     * @throws org.openbase.jul.exception.NotAvailableException is thrown in case the given serviceType is null.
     */
    public static String getServiceStateName(final ServiceType serviceType) throws NotAvailableException {
        try {
            if (serviceType == null) {
                assert false;
                throw new NotAvailableException("ServiceState");
            }
            return StringProcessor.transformUpperCaseToPascalCase(serviceType.name()).replaceAll("Service", "");
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("ServiceStateName", ex);
        }
    }

    /**
     * Method returns the state name of the appurtenant service.
     *
     * @param template The service template.
     *
     * @return The state type name as string.
     *
     * @throws org.openbase.jul.exception.NotAvailableException is thrown in case the given template is null.
     */
    public static String getServiceStateName(final ServiceTemplate template) throws NotAvailableException {
        try {
            if (template == null) {
                assert false;
                throw new NotAvailableException("ServiceTemplate");
            }
            return getServiceStateName(template.getServiceType());
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("ServiceStateName", ex);
        }
    }

    /**
     * Method returns a collection of service state values.
     *
     * @param serviceType the service type to identify the service state class.
     *
     * @return a collection of enum values of the service state.
     *
     * @throws NotAvailableException is thrown in case the referred service state does not contain any state values.
     */
    public static Collection<ProtocolMessageEnum> getServiceStateEnumValues(final ServiceType serviceType) throws NotAvailableException {
        try {
            return getServiceStateEnumValues(getServiceStateClass(serviceType));
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException(getServiceBaseName(serviceType), "ServiceStateValues", ex);
        }
    }

    /**
     * Method returns a collection of service state values.
     *
     * @param communicationType the communication type to identify the service state class.
     *
     * @return a collection of enum values of the service state.
     *
     * @throws NotAvailableException is thrown in case the referred service state does not contain any state values.
     */
    public static Collection<ProtocolMessageEnum> getServiceStateEnumValues(final CommunicationType communicationType) throws NotAvailableException {
        try {
            return getServiceStateEnumValues(getServiceStateClass(communicationType));
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException(communicationType.name(), "ServiceStateValues", ex);
        }
    }

    /**
     * Method returns a collection of service state values.
     *
     * @param serviceStateClass the service state class to resolve the values.
     *
     * @return a collection of enum values of the service state.
     *
     * @throws NotAvailableException is thrown in case the referred service state does not contain any state values.
     */
    public static Collection<ProtocolMessageEnum> getServiceStateEnumValues(final Class<? extends Message> serviceStateClass) throws NotAvailableException {
        try {
            for (Class<?> declaredClass : serviceStateClass.getDeclaredClasses()) {
                if (declaredClass.getSimpleName().equals("State")) {
                    try {
                        return Arrays.asList((ProtocolMessageEnum[]) (declaredClass.getMethod("values").invoke(null)));
                    } catch (IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException | InvocationTargetException ex) {
                        throw new CouldNotPerformException("Could not extract values of Enum[" + declaredClass.getSimpleName() + "]");
                    }
                }
            }
            throw new InvalidStateException("Class[" + serviceStateClass.getSimpleName() + "] does not provide a service state enum!");
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException(serviceStateClass.getSimpleName(), "ServiceStateEnumValues", ex);
        }
    }

    /**
     * Method generates a new service state builder related to the given {@code comman}.
     *
     * @param communicationType the communication type of the service state.
     *
     * @throws CouldNotPerformException is thrown if something went wrong during the generation.
     */
    public static Message.Builder generateServiceStateBuilder(final CommunicationType communicationType) throws CouldNotPerformException {
        try {
            // create new service state builder
            return (Message.Builder) Services.getServiceStateClass(communicationType).getMethod("newBuilder").invoke(null);
        } catch (final IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException | InvocationTargetException | NotAvailableException | ClassCastException ex) {
            throw new CouldNotPerformException("Could not generate service state builder from CommunicationType[" + communicationType + "]!", ex);
        }
    }

    /**
     * Method generates a new service state builder related to the given {@code serviceType}.
     *
     * @param serviceType the service type of the service state.
     *
     * @throws CouldNotPerformException is thrown if something went wrong during the generation.
     */
    public static Message.Builder generateServiceStateBuilder(final ServiceType serviceType) throws CouldNotPerformException {
        try {
            // create new service state builder
            return (Message.Builder) Services.getServiceStateClass(serviceType).getMethod("newBuilder").invoke(null);
        } catch (final IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException | InvocationTargetException | NotAvailableException | ClassCastException ex) {
            throw new CouldNotPerformException("Could not generate service state builder from ServiceType[" + serviceType + "]!", ex);
        }
    }

    /**
     * Method generates a new service state builder related to the given {@code serviceType} and initializes this instance with the given {@code stateValue}.
     *
     * @param <SC>        the service class of the service state.
     * @param <SV>        the state enum of the service.
     * @param serviceType the service type of the service state.
     * @param stateValue  a compatible state value related to the given service state.
     *
     * @return a new service state initialized with the state value.
     *
     * @throws CouldNotPerformException is thrown in case the given arguments are not compatible with each other or something else went wrong during the build.
     */
    public static <SC extends Message.Builder, SV extends ProtocolMessageEnum> SC generateServiceStateBuilder(final ServiceType serviceType, SV stateValue) throws CouldNotPerformException {
        try {
            // create new service state builder
            Message.Builder serviceStateBuilder = generateServiceStateBuilder(serviceType);

            // set service state value
            serviceStateBuilder.getClass().getMethod("setValue", stateValue.getClass()).invoke(serviceStateBuilder, stateValue);

            // return
            return (SC) serviceStateBuilder;
        } catch (final IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException | InvocationTargetException | NotAvailableException | ClassCastException ex) {
            throw new CouldNotPerformException("Could not build service state!", ex);
        }
    }

    /**
     * Method generates a new service state builder related to the given {@code serviceType} and initializes this instance with the given {@code stateValue}.
     *
     * @param <SC>              the service class of the service state.
     * @param <SV>              the state enum of the service.
     * @param communicationType the communication type of the service state.
     * @param stateValue        a compatible state value related to the given service state.
     *
     * @return a new service state initialized with the state value.
     *
     * @throws CouldNotPerformException is thrown in case the given arguments are not compatible with each other or something else went wrong during the build.
     */
    public static <SC extends Message.Builder, SV extends ProtocolMessageEnum> SC generateServiceStateBuilder(final CommunicationType communicationType, SV stateValue) throws CouldNotPerformException {
        try {
            // create new service state builder
            Message.Builder serviceStateBuilder = generateServiceStateBuilder(communicationType);

            // set service state value
            serviceStateBuilder.getClass().getMethod("setValue", stateValue.getClass()).invoke(serviceStateBuilder, stateValue);

            // return
            return (SC) serviceStateBuilder;
        } catch (final IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException | InvocationTargetException | NotAvailableException | ClassCastException ex) {
            throw new CouldNotPerformException("Could not build service state!", ex);
        }
    }


    /**
     * Method builds a new service state related to the given {@code serviceType} and initializes this instance with the given {@code stateValue}.
     *
     * @param <SC>        the service class of the service state.
     * @param <SV>        the state enum of the service.
     * @param serviceType the service type of the service state.
     * @param stateValue  a compatible state value related to the given service state.
     *
     * @return a new service state initialized with the state value.
     *
     * @throws CouldNotPerformException is thrown in case the given arguments are not compatible with each other or something else went wrong during the build.
     */
    public static <SC extends Message, SV extends ProtocolMessageEnum> SC buildServiceState(final ServiceType serviceType, SV stateValue) throws CouldNotPerformException {
        try {
            // create new service state builder with new state and build.
            return (SC) generateServiceStateBuilder(serviceType, stateValue).build();
        } catch (IllegalArgumentException | SecurityException | NotAvailableException | ClassCastException ex) {
            throw new CouldNotPerformException("Could not build service state!", ex);
        }
    }

    /**
     * @deprecated since v2.0 and will be removed in v3.0. Please use {@code getServiceStateClass(final ServiceType serviceType)} instead.
     */
    @Deprecated
    public static Class<? extends Message> detectServiceDataClass(final ServiceType serviceType) throws NotAvailableException {
        return getServiceStateClass(serviceType);
    }

    /**
     * Method detects and returns the service state class.
     *
     * @param serviceType the given service type to resolve the class.
     *
     * @return the service state class.
     *
     * @throws NotAvailableException is thrown in case the class could not be detected.
     */
    public static Class<? extends Message> getServiceStateClass(final ServiceType serviceType) throws NotAvailableException {
        try {
            return getServiceStateClass(Registries.getTemplateRegistry().getServiceTemplateByType(serviceType).getCommunicationType());
        } catch (CouldNotPerformException e) {
            throw new NotAvailableException("CommunicationType for ServiceType[" + serviceType + "]");
        }
    }

    /**
     * Method detects and returns the service state class.
     *
     * @param communicationType the communication type to resolve the service state class.
     *
     * @return the service state class.
     *
     * @throws NotAvailableException is thrown in case the class could not be detected.
     */
    public static Class<? extends Message> getServiceStateClass(final CommunicationType communicationType) throws NotAvailableException {
        String serviceStateName;
        try {
            if (communicationType == CommunicationType.UNKNOWN) {
                throw new InvalidStateException("CommunicationType is not configured in ServiceTemplate!");
            }
            serviceStateName = StringProcessor.transformUpperCaseToPascalCase(communicationType.name());
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("CommunicationType", communicationType.name());
        }

        final String serviceClassName = SERVICE_STATE_PACKAGE.getName() + "." + serviceStateName + "Type$" + serviceStateName;
        try {
            return (Class<? extends Message>) Class.forName(serviceClassName);
        } catch (NullPointerException | ClassNotFoundException | ClassCastException ex) {
            throw new NotAvailableException("ServiceStateClass", serviceClassName, new CouldNotPerformException("Could not detect class!", ex));
        }
    }

    public static Method detectServiceMethod(final ServiceType serviceType, final ServicePattern servicePattern, final Class instanceClass, final Class... argumentClasses) throws CouldNotPerformException {
        return detectServiceMethod(serviceType, servicePattern, ServiceTempus.CURRENT, instanceClass, argumentClasses);
    }

    public static Method detectServiceMethod(final ServiceType serviceType, final ServicePattern servicePattern, final ServiceTempus serviceTempus, final Class instanceClass, final Class... argumentClasses) throws CouldNotPerformException {
        return detectServiceMethod(serviceType, getServiceMethodPrefix(servicePattern), serviceTempus, instanceClass, argumentClasses);
    }

    public static Method detectServiceMethod(final ServiceType serviceType, final String serviceMethodPrefix, final ServiceTempus serviceTempus, final Class instanceClass, final Class... argumentClasses) throws CouldNotPerformException {
        String messageName = "?";
        try {
            messageName = serviceMethodPrefix + getServiceStateName(serviceType) + StringProcessor.transformUpperCaseToPascalCase(serviceTempus.name().replace(ServiceTempus.CURRENT.name(), ""));
            return instanceClass.getMethod(messageName, argumentClasses);
        } catch (NoSuchMethodException | SecurityException ex) {
            throw new CouldNotPerformException("Could not detect service method[" + messageName + "]!", ex);
        }
    }

    public static Method detectServiceMethod(final ServiceDescription description, final Class instanceClass, final Class... argumentClasses) throws CouldNotPerformException {
        return detectServiceMethod(description, ServiceTempus.CURRENT, instanceClass, argumentClasses);
    }

    public static Method detectServiceMethod(final ServiceDescription description, final ServiceTempus serviceTempus, final Class instanceClass, final Class... argumentClasses) throws CouldNotPerformException {
        return detectServiceMethod(description.getServiceType(), description.getPattern(), serviceTempus, instanceClass, argumentClasses);
    }

    public static Object invokeServiceMethod(final ServiceDescription description, final Service instance, final Object... arguments) throws CouldNotPerformException {
        return invokeServiceMethod(description, ServiceTempus.CURRENT, instance, arguments);
    }

    public static Object invokeServiceMethod(final ServiceDescription description, final ServiceTempus serviceTempus, final Service instance, final Object... arguments) throws CouldNotPerformException {
        try {
            return detectServiceMethod(description, serviceTempus, instance.getClass(), getArgumentClasses(arguments)).invoke(instance, arguments);
        } catch (IllegalAccessException | ExceptionInInitializerError ex) {
            throw new NotSupportedException("ServiceType[" + description.getServiceType().name() + "] with Pattern[" + description.getPattern() + "]", instance, ex);
        } catch (NullPointerException ex) {
            throw new CouldNotPerformException("Invocation failed because given instance is not available!", ex);
        } catch (InvocationTargetException ex) {
            if (ex.getTargetException() instanceof CouldNotPerformException) {
                throw (CouldNotPerformException) ex.getTargetException();
            } else {
                throw new CouldNotPerformException("Invocation failed!", ex.getTargetException());
            }
        }
    }

    public static Object invokeServiceMethod(final ServiceType serviceType, final ServicePattern servicePattern, final Object instance, final Object... arguments) throws CouldNotPerformException, IllegalArgumentException {
        return invokeServiceMethod(serviceType, servicePattern, ServiceTempus.CURRENT, instance, arguments);
    }

    public static Object invokeServiceMethod(final ServiceType serviceType, final ServicePattern servicePattern, final ServiceTempus serviceTempus, final Object instance, final Object... arguments) throws CouldNotPerformException, IllegalArgumentException {
        try {
            return detectServiceMethod(serviceType, servicePattern, serviceTempus, instance.getClass(), getArgumentClasses(arguments)).invoke(instance, arguments);
        } catch (IllegalAccessException | ExceptionInInitializerError | ClassCastException ex) {
            throw new NotSupportedException("ServiceType[" + serviceType.name() + "] with Pattern[" + servicePattern + "]", instance, ex);
        } catch (NullPointerException ex) {
            throw new CouldNotPerformException("Invocation failed because given instance is not available!", ex);
        } catch (InvocationTargetException ex) {
            if (ex.getTargetException() instanceof CouldNotPerformException) {
                throw (CouldNotPerformException) ex.getTargetException();
            } else {
                throw new CouldNotPerformException("Invocation failed!", ex.getTargetException());
            }
        }
    }

    public static Message invokeProviderServiceMethod(final ServiceType serviceType, final Object instance) throws CouldNotPerformException, IllegalArgumentException {
        return (Message) invokeServiceMethod(serviceType, ServicePattern.PROVIDER, instance);
    }

    public static Message invokeProviderServiceMethod(final ServiceType serviceType, final ServiceTempus serviceTempus, final Object instance) throws CouldNotPerformException, IllegalArgumentException {
        return (Message) invokeServiceMethod(serviceType, ServicePattern.PROVIDER, serviceTempus, instance);
    }

    public static Object invokeOperationServiceMethod(final ServiceType serviceType, final Object instance, final Object... arguments) throws CouldNotPerformException, IllegalArgumentException {
        return invokeServiceMethod(serviceType, ServicePattern.OPERATION, instance, arguments);
    }

    public static Class[] getArgumentClasses(final Object[] arguments) {
        Class[] classes = new Class[arguments.length];
        for (int i = 0; i < classes.length; i++) {
            classes[i] = arguments[i].getClass();
        }
        return classes;
    }

    public static String getServiceFieldName(final ServiceType serviceType, final ServiceTempus serviceTempus) {
        String result = serviceType.name().replace(Service.SERVICE_LABEL.toUpperCase(), "").toLowerCase();
        switch (serviceTempus) {
            case REQUESTED:
            case LAST:
                // add service tempus postfix
                result += serviceTempus.name().toLowerCase();
                break;
            case CURRENT:
            case UNKNOWN:
                // remove underscore at the end
                result = result.substring(0, result.length() - 1);
                break;
        }
        return result;
    }

    public static Boolean hasServiceState(final ServiceType serviceType, final ServiceTempus serviceTempus, final MessageOrBuilder instance, final Object... arguments) throws CouldNotPerformException, IllegalArgumentException {
        try {
            return (Boolean) detectServiceMethod(serviceType, "has", serviceTempus, instance.getClass(), getArgumentClasses(arguments)).invoke(instance, arguments);
        } catch (IllegalAccessException | ExceptionInInitializerError ex) {
            throw new NotSupportedException("ServiceType[" + serviceType.name() + "] not provided by Message[" + instance.getClass().getSimpleName() + "]!", instance, ex);
        } catch (NullPointerException ex) {
            throw new CouldNotPerformException("Invocation failed because given instance is not available!", ex);
        } catch (InvocationTargetException ex) {
            if (ex.getTargetException() instanceof CouldNotPerformException) {
                throw (CouldNotPerformException) ex.getTargetException();
            } else {
                throw new CouldNotPerformException("Invocation failed!", ex.getTargetException());
            }
        }
    }

    public static void verifyServiceState(final MessageOrBuilder serviceState) throws VerificationFailedException {

        if (serviceState == null) {
            throw new VerificationFailedException(new NotAvailableException("ServiceState"));
        }

        final Method valueMethod;
        try {
            valueMethod = serviceState.getClass().getMethod("getValue");
            try {
                verifyServiceStateValue((Enum) valueMethod.invoke(serviceState));
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | ClassCastException ex) {
                ExceptionPrinter.printHistory("Operation service verification phase failed of ServiceState[ " + serviceState.getClass().getSimpleName() + "]!", ex, LOGGER);
            }
        } catch (NoSuchMethodException ex) {
            // service state does contain any value so verification is not needed.
        }
    }

    public static void verifyServiceStateValue(final Enum value) throws VerificationFailedException {
        if (value == null) {
            throw new VerificationFailedException(new NotAvailableException("ServiceStateValue"));
        }

        if (value.name().equals("UNKNOWN")) {
            throw new VerificationFailedException(value.getClass().getSimpleName() + ".UNKNOWN" + " is an invalid operation service state!");
        }
    }

    /**
     * Verification of the given service state inclusive consistency revalidation.
     * This means field are recalculated in case they are not consistent against each other.
     *
     * @param serviceState the state type to validate.
     *
     * @return the given state or an updated version of it.
     *
     * @throws VerificationFailedException is thrown if the state is invalid and no repair functions are available.
     */
    public static Message verifyAndRevalidateServiceState(final Message serviceState) throws VerificationFailedException {
        try {
            try {
                final Object verifiedState = detectServiceStateVerificationMethod(serviceState).invoke(null, serviceState);
                if (verifiedState != null && verifiedState instanceof Message) {
                    return (Message) verifiedState;
                }
                return serviceState;
            } catch (NotAvailableException ex) {
                ExceptionPrinter.printHistory("Verification of ServiceState[ " + serviceState.getClass().getSimpleName() + "] skipped because verification method not supported yet.!", ex, LOGGER, LogLevel.DEBUG);
            } catch (InvocationTargetException ex) {
                if (ex.getTargetException() instanceof VerificationFailedException) {
                    throw (VerificationFailedException) ex.getTargetException();
                } else {
                    throw ex;
                }
            }
        } catch (VerificationFailedException ex) {
            throw ex;
        } catch (NullPointerException | IllegalAccessException | ExceptionInInitializerError | CouldNotPerformException | InvocationTargetException ex) {
            ExceptionPrinter.printHistory(new FatalImplementationErrorException("Verification of service state could no be performed!", Services.class, ex), LOGGER, LogLevel.WARN);
        }
        return serviceState;
    }

    public static Method detectServiceStateVerificationMethod(final Message serviceState) throws CouldNotPerformException {
        String methodName = "?";
        try {
            methodName = "verify" + serviceState.getClass().getSimpleName();
            return detectProviderServiceInterface(serviceState).getMethod(methodName, serviceState.getClass());
        } catch (SecurityException | ClassNotFoundException ex) {
            throw new CouldNotPerformException("Could not detect service method[" + methodName + "]!", ex);
        } catch (NoSuchMethodException ex) {
            throw new NotAvailableException("service state verification method", ex);
        }
    }

    public static Class detectProviderServiceInterface(final Message serviceState) throws ClassNotFoundException {
        return Class.forName(ProviderService.class.getPackage().getName() + "." + serviceState.getClass().getSimpleName() + ProviderService.class.getSimpleName());
    }

    public static Class detectOperationServiceInterface(final Message serviceState) throws ClassNotFoundException {
        return Class.forName(OperationService.class.getPackage().getName() + "." + serviceState.getClass().getSimpleName() + OperationService.class.getSimpleName());
    }

    public static Class detectConsumerServiceInterface(final Message serviceState) throws ClassNotFoundException {
        return Class.forName(ConsumerService.class.getPackage().getName() + "." + serviceState.getClass().getSimpleName() + ConsumerService.class.getSimpleName());
    }

    /**
     * Method returns the action which is responsible for the given state.
     *
     * @param serviceState the state used to resolve the responsible action.
     *
     * @return the responsible action.
     *
     * @throws NotAvailableException is thrown if the related action can not be determine.
     */
    public static ActionDescription getResponsibleAction(final MessageOrBuilder serviceState) throws NotAvailableException {
        try {
            return (ActionDescription) serviceState.getField(getResponsibleActionField(serviceState));
        } catch (NotAvailableException ex) {
            throw new NotAvailableException("ActionDescription", ex);
        }
    }

    /**
     * Method set the responsible action of the service state.
     *
     * @param responsibleAction the action to setup.
     * @param serviceState      the message which is updated with the given responsible action.
     * @param <M>               the type of the service state message.
     *
     * @return the modified message instance.
     *
     * @throws NotAvailableException is thrown if the builder does not provide a responsible action.
     */
    public static <M extends Message> M setResponsibleAction(final ActionDescription responsibleAction, final M serviceState) throws NotAvailableException {
        return (M) setResponsibleAction(responsibleAction, serviceState.toBuilder()).build();
    }

    /**
     * Method set the responsible action of the service state.
     *
     * @param responsibleAction   the action to setup.
     * @param serviceStateBuilder the builder which is updated with the given responsible action.
     * @param <B>                 the type of the service state builder.
     *
     * @return the modified builder instance.
     *
     * @throws NotAvailableException is thrown if the builder does not provide a responsible action.
     */
    public static <B extends Message.Builder> B setResponsibleAction(final ActionDescription responsibleAction, final B serviceStateBuilder) throws NotAvailableException {
        serviceStateBuilder.setField(ProtoBufFieldProcessor.getFieldDescriptor(serviceStateBuilder, Service.RESPONSIBLE_ACTION_FIELD_NAME), responsibleAction);
        return serviceStateBuilder;
    }

    public static Class<?> loadOperationServiceClass(final ServiceType serviceType) throws ClassNotFoundException {
        final String className = StringProcessor.transformUpperCaseToPascalCase(serviceType.name()).replace("Service", "") + OperationService.class.getSimpleName();
        final String packageString = OperationService.class.getPackage().getName();
        return Services.class.getClassLoader().loadClass(packageString + "." + className);
    }

    public static boolean hasResponsibleAction(final MessageOrBuilder serviceState) throws NotAvailableException {
        return serviceState.hasField(getResponsibleActionField(serviceState));
    }

    public static <B extends Message.Builder> B clearResponsibleAction(final B serviceStateBuilder) throws NotAvailableException {
        serviceStateBuilder.clearField(getResponsibleActionField(serviceStateBuilder));
        return serviceStateBuilder;
    }

    public static FieldDescriptor getResponsibleActionField(final MessageOrBuilder serviceState) throws NotAvailableException {
        FieldDescriptor fieldDescriptor = ProtoBufFieldProcessor.getFieldDescriptor(serviceState, Service.RESPONSIBLE_ACTION_FIELD_NAME);
        if (fieldDescriptor == null) {
            throw new NotAvailableException("responsible action for " + serviceState.getClass().getSimpleName());
        }
        return fieldDescriptor;
    }

    public static List<String> generateServiceProviderStringRepresentation(Object serviceProvider, ServiceType serviceType) throws CouldNotPerformException {
        return generateServiceStateStringRepresentation(Services.invokeProviderServiceMethod(serviceType, serviceProvider), serviceType);
    }

//    public static List<String> generateServiceStateStringRepresentation(Message serviceState, ServiceType serviceType) throws CouldNotPerformException {
//
//        final List<String> states = new ArrayList<>();
//
//        for (Entry<FieldDescriptor, Object> entry : serviceState.getAllFields().entrySet()) {
//
//            // filter empty repeated fields
//            if (!entry.getKey().isRepeated() && !serviceState.hasField(entry.getKey()) || entry.getValue() == null) {
//                continue;
//            }
//
//            String stateName = entry.getKey().getName();
//            String stateValue = entry.getValue().toString();
//            if (stateName == null || entry.getValue() == null) {
//                continue;
//            }
//
//            String timestamp;
//            try {
//                timestamp = Long.toString(TimestampProcessor.getTimestamp(serviceState, TimeUnit.MILLISECONDS));
//            } catch (NotAvailableException ex) {
//                timestamp = "na";
//            }
//
//            switch (stateValue) {
//                case "":
//                case "NaN":
//                    continue;
//                default:
//                    break;
//            }
//
//            switch (stateName) {
//                case "":
//                case "last_value_occurrence":
//                case "timestamp":
//                case "responsible_action":
//                    continue;
//                case "color":
//                    final HSBColor hsbColor = ((Color) entry.getValue()).getHsbColor();
//                    stateValue = hsbColor.getHue() + ", " + hsbColor.getSaturation() + ", " + hsbColor.getBrightness();
//                    break;
//                case "value":
//                    break;
//            }
//            states.add(serviceType.name().toLowerCase() + ", " + timestamp + ", [" + stateValue.toLowerCase() + "]");
//        }
//        return states;
//    }

    public static List<String> getServiceStateFieldDataTypes(final ServiceType serviceType) throws CouldNotPerformException {
        return getFieldDataTypes(Services.generateServiceStateBuilder(serviceType).build());
    }

    public static List<String> getFieldDataTypes(final Message messagePrototype) throws CouldNotPerformException {
        final List<String> dataTypes = new ArrayList<>();
        for (FieldDescriptor fieldDescriptor : messagePrototype.getDescriptorForType().getFields()) {
            String stateName = fieldDescriptor.getName();
            String stateType = fieldDescriptor.getType().toString().toLowerCase();

            // filter invalid states
            if (stateName == null || stateType == null) {
                LOGGER.warn("Could not detect datatype of " + stateName);
            }

            // filter general service fields
            switch (stateName) {
                case "last_value_occurrence":
                case "timestamp":
                case "responsible_action":
                case "type":
                case "rgb_color":
                case "frame_id":
                    continue;
            }

            // filter data units
            if (stateName.endsWith("data_unit")) {
                continue;
            }

            //System.out.println("name: "+ stateName);

            if (fieldDescriptor.getType() == Type.MESSAGE) {
                if (fieldDescriptor.isRepeated()) {
                    List<String> types = new ArrayList<>();
                    for (int i = 0; i < messagePrototype.getRepeatedFieldCount(fieldDescriptor); i++) {
                        final Object repeatedFieldEntry = messagePrototype.getRepeatedField(fieldDescriptor, i);
                        if (repeatedFieldEntry instanceof Message) {
                            types.add("[" + getFieldDataTypes((Message) repeatedFieldEntry).toString() + "]");
                        }
                        types.add(repeatedFieldEntry.getClass().getSimpleName());
                    }
                    stateType = types.toString().toLowerCase();
                } else {
                    stateType = getFieldDataTypes((Message) messagePrototype.getField(fieldDescriptor)).toString();
                }
            }

            dataTypes.add(fieldDescriptor.getName() + "=" + stateType);
        }
        return dataTypes;
    }

    public static List<String> generateServiceStateStringRepresentation(Message serviceState, ServiceType serviceType) throws CouldNotPerformException {
        final List<String> values = new ArrayList<>();
        String timestamp;
        try {
            timestamp = Long.toString(TimestampProcessor.getTimestamp(serviceState, TimeUnit.MILLISECONDS));
        } catch (NotAvailableException ex) {
            timestamp = "-1";
        }
        for (String stateValue : resolveStateValue(serviceState)) {
            values.add(serviceType.name().toLowerCase() + ", " + timestamp + ", " + stateValue);
        }
        return values;
    }

    public static List<String> resolveStateValue(Message serviceState) throws CouldNotPerformException {
        final List<String> stateValues = new ArrayList<>();
        for (FieldDescriptor fieldDescriptor : serviceState.getDescriptorForType().getFields()) {
            String stateName = fieldDescriptor.getName();
            String stateType = fieldDescriptor.getType().toString().toLowerCase();

            // filter invalid states
            if (stateName == null || stateType == null) {
                LOGGER.warn("Could not detect datatype of " + stateName);
            }

            // filter general service fields
            switch (stateName) {
                case "last_value_occurrence":
                case "timestamp":
                case "responsible_action":
                case "type":
                case "rgb_color":
                case "frame_id":
                    continue;
            }

            // filter data units
            if (stateName.endsWith("data_unit")) {
                continue;
            }

            String stateValue = serviceState.getField(fieldDescriptor).toString();

            try {
                if (fieldDescriptor.getType() == Type.MESSAGE) {
                    if (fieldDescriptor.isRepeated()) {
                        List<String> types = new ArrayList<>();

                        for (int i = 0; i < serviceState.getRepeatedFieldCount(fieldDescriptor); i++) {
                            final Object repeatedFieldEntry = serviceState.getRepeatedField(fieldDescriptor, i);
                            if (repeatedFieldEntry instanceof Message) {
                                types.add("[" + resolveStateValue((Message) repeatedFieldEntry).toString() + "]");
                            }
                            types.add(repeatedFieldEntry.toString());
                        }
                        stateType = types.toString().toLowerCase();
                    } else {
                        stateValue = resolveStateValue((Message) serviceState.getField(fieldDescriptor)).toString();
                    }
                }
            } catch (InvalidStateException ex) {
                LOGGER.warn("Could not process value of " + fieldDescriptor.getName());
                continue;
            }

            // filter values
            switch (stateValue) {
                case "":
                case "NaN":
                    continue;
                default:
                    break;
            }

            stateValues.add(fieldDescriptor.getName() + "=" + stateValue.toLowerCase());
        }
        return stateValues;
    }

    /**
     * Test if two service states are equal to each other. This test iterates over all fields of serviceState1 and
     * tests if they have the same value in serviceState2. It ignores the following utility fields:
     * <ul>
     * <li>timestamp</li>
     * <li>responsible_action</li>
     * <li>last</li>
     * <li>state_transaction_reference</li>
     * </ul>
     *
     * @param serviceState1 the first state compared.
     * @param serviceState2 the second state compared.
     *
     * @return if all fields except the fields mentioned above are equal.
     */
    public static boolean equalServiceStates(final Message serviceState1, final Message serviceState2) {
        // both states are null so return true
        if (serviceState1 == null && serviceState2 == null) {
            return true;
        }

        // only one state is null so return false
        if (serviceState1 == null || serviceState2 == null) {
            return false;
        }

        if (!serviceState1.getClass().equals(serviceState2.getClass())) {
            return false;
        }

        //TODO: for performance reasons it would be nice if all the fields skipped below had the same field number in all
        // service states. It would reduce the string comparison to an integer comparison.
        for (final Descriptors.FieldDescriptor field : serviceState1.getDescriptorForType().getFields()) {
            if (field.getName().equals(ServiceStateProcessor.FIELD_NAME_LAST_VALUE_OCCURRENCE)) {
                continue;
            }

            if (field.getName().equals("state_transaction_reference")) {
                continue;
            }

            // ignore timestamps
            if (field.getName().equals(TimestampProcessor.TIMESTAMP_FIELD_NAME)) {
                continue;
            }

            // ignore responsible action
            if (field.getName().equals(Service.RESPONSIBLE_ACTION_FIELD_NAME)) {
                continue;
            }

            if ((field.isRepeated() || (serviceState1.hasField(field) && serviceState2.hasField(field))) && !(serviceState1.getField(field).equals(serviceState2.getField(field)))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Convert a generic service state of a service type to one of its super service types. This is done by resolving
     * the provider service interface belonging to the service type and calling the toSuperServiceState method. E.g.
     * converting a ColorState as a COLOR_STATE_SERVICE to a POWER_STATE_SERVICE will result in a call to the method
     * {@link org.openbase.bco.dal.lib.layer.service.provider.ColorStateProviderService#toPowerState(ColorState)}.
     *
     * @param serviceType      the service type of the service state.
     * @param serviceState     the service state to be converted.
     * @param superServiceType the super service type describing the state into which to convert.
     *
     * @return a state matching the super service type.
     *
     * @throws CouldNotPerformException if the conversion fails because of invalid arguments or because conversion methods
     *                                  are not available.
     */
    public static Message convertToSuperState(final ServiceType serviceType, final Message serviceState, final ServiceType superServiceType) throws CouldNotPerformException {
        try {
            // retrieve provider service class
            final String simpleClassName = StringProcessor.transformUpperCaseToPascalCase(serviceType.name()).replace(Service.class.getSimpleName(), ProviderService.class.getSimpleName());
            final String className = ProviderService.class.getPackage().getName() + "." + simpleClassName;
            final Class<?> providerClass = Services.class.getClassLoader().loadClass(className);

            final String methodName = "to" + StringProcessor.transformToPascalCase(superServiceType.name()).replace(Service.class.getSimpleName(), "");
            final Method method = providerClass.getMethod(methodName, serviceState.getClass());

            return (Message) method.invoke(null, serviceState);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassCastException ex) {
            throw new CouldNotPerformException("Could not convert state[" + serviceState.getClass().getSimpleName() + "] of serviceType[" + serviceType.name() + "] to state of superServiceType[" + superServiceType.name() + "]", ex);
        }
    }
}


