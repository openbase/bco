package org.openbase.bco.dal.lib.layer.service;

/*-
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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

import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.ProtocolMessageEnum;
import org.openbase.bco.dal.lib.action.Action;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.FatalImplementationErrorException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.protobuf.ProtoBufBuilderProcessor;
import org.openbase.jul.extension.protobuf.processing.ProtoBufFieldProcessor;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.service.ServiceConfigType.ServiceConfig;
import org.openbase.type.domotic.service.ServiceStateDescriptionType.ServiceStateDescription;
import org.openbase.type.domotic.service.ServiceStateDescriptionType.ServiceStateDescriptionOrBuilder;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.language.MultiLanguageTextType.MultiLanguageText;
import org.openbase.type.timing.TimestampType.Timestamp;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Supplier;

public class ServiceStateProcessor {

    public static final String INTERNAL_MAP_CLASS_NAME = "$MapFieldEntry";
    public static final String FIELD_NAME_LAST_VALUE_OCCURRENCE = "last_value_occurrence";
    public static final String FIELD_NAME_TIMESTAMP = "timestamp";
    public static final String FIELD_NAME_KEY = "key";
    public static final String FIELD_NAME_VALUE = "value";
    public static final String FIELD_NAME_RESPONSIBLE_ACTION = "responsible_action";

    private static final ServiceJSonProcessor JSON_PROCESSOR = new ServiceJSonProcessor();

    /**
     * Method returns the timestamp of the latest occurrence of the given service state value.
     *
     * @param enumStateValue the value to identify the state.
     * @param serviceState   the service state type which provides the occurrence list.
     *
     * @return the timestamp of the last occurrence.
     *
     * @throws NotAvailableException is thrown if the timestamp is not available.
     */
    public static Timestamp getLatestValueOccurrence(final ProtocolMessageEnum enumStateValue, MessageOrBuilder serviceState) throws NotAvailableException {
        return getLatestValueOccurrence(enumStateValue.getValueDescriptor(), serviceState);
    }

    /**
     * Method returns the timestamp of the latest occurrence of the given service state value.
     *
     * @param enumValueDescriptor the enum value descriptor of the value to identify the state.
     * @param serviceState        the service state type which provides the occurrence list.
     *
     * @return the timestamp of the last occurrence.
     *
     * @throws NotAvailableException is thrown if the timestamp is not available.
     */
    public static Timestamp getLatestValueOccurrence(final EnumValueDescriptor enumValueDescriptor, MessageOrBuilder serviceState) throws NotAvailableException {
        FieldDescriptor mapFieldDescriptor = serviceState.getDescriptorForType().findFieldByName("last_value_occurrence");
        if (mapFieldDescriptor == null) {
            throw new NotAvailableException("Field[ast_value_occurrence] does not exist for type " + serviceState.getClass().getName());
        }
        return (Timestamp) ProtoBufFieldProcessor.getMapEntry(enumValueDescriptor, mapFieldDescriptor, serviceState);
    }

    /**
     * This method updates the last occurrence of the given service state value.
     * <p>
     * Note: This action takes only place if the given timestamp is newer than the currently stored one.
     *
     * @param enumStateValue the state value which as been occurred.
     * @param timestamp      the timestamp of the occurrence.
     * @param serviceState   the service state message which holds the service value.
     *
     * @throws CouldNotPerformException is thrown if the update could not be performed.
     */
    public static void updateLatestValueOccurrence(final ProtocolMessageEnum enumStateValue, long timestamp, Message.Builder serviceState) throws CouldNotPerformException {
        updateLatestValueOccurrence(enumStateValue, Timestamp.newBuilder().setTime(timestamp).build(), serviceState);
    }

    /**
     * This method updates the last occurrence of the given service state value.
     * <p>
     * Note: This action takes only place if the given timestamp is newer than the currently stored one.
     *
     * @param enumStateValue the state value which as been occurred.
     * @param timestamp      the timestamp of the occurrence.
     * @param serviceState   the service state message which holds the service value.
     *
     * @throws CouldNotPerformException is thrown if the update could not be performed.
     */
    public static void updateLatestValueOccurrence(final ProtocolMessageEnum enumStateValue, final Timestamp timestamp, Message.Builder serviceState) throws CouldNotPerformException {
        updateLatestValueOccurrence(enumStateValue.getValueDescriptor(), timestamp, serviceState);
    }

    /**
     * This method updates the last occurrence of the given service state value.
     * <p>
     * Note: This action takes only place if the given timestamp is newer than the currently stored one.
     *
     * @param enumValueDescriptor the enum value descriptor of the state value which as been occurred.
     * @param timestamp           the timestamp of the occurrence.
     * @param serviceState        the service state message which holds the service value.
     *
     * @throws CouldNotPerformException is thrown if the update could not be performed.
     */
    public static void updateLatestValueOccurrence(final EnumValueDescriptor enumValueDescriptor, long timestamp, Message.Builder serviceState) throws CouldNotPerformException {
        updateLatestValueOccurrence(enumValueDescriptor, Timestamp.newBuilder().setTime(timestamp).build(), serviceState);
    }

    /**
     * This method updates the last occurrence of the given service state value.
     * <p>
     * Note: This action takes only place if the given timestamp is newer than the currently stored one.
     *
     * @param enumValueDescriptor the enum value descriptor of the value which as been occurred.
     * @param timestamp           the timestamp of the occurrence.
     * @param serviceState        the service state message which holds the service value.
     *
     * @throws CouldNotPerformException is thrown if the update could not be performed.
     */
    public static void updateLatestValueOccurrence(final EnumValueDescriptor enumValueDescriptor, final Timestamp timestamp, Message.Builder serviceState) throws CouldNotPerformException {

        // skip update if given timestamp is outdated.
        try {
            if (timestamp.getTime() <= getLatestValueOccurrence(enumValueDescriptor, serviceState).getTime()) {
                return;
            }
        } catch (NotAvailableException ex) {
            // no entry for state available, so continue with the update...
        }

        final Message entryMessage;

        try {
            final Class mapFieldEntryClass = Class.forName(serviceState.getClass().getDeclaringClass().getName() + INTERNAL_MAP_CLASS_NAME);
            final Message.Builder entryMessageBuilder = (Message.Builder) mapFieldEntryClass.getMethod("newBuilder").invoke(null);

            final FieldDescriptor keyDescriptor = entryMessageBuilder.build().getDescriptorForType().findFieldByName(FIELD_NAME_KEY);
            final FieldDescriptor valueDescriptor = entryMessageBuilder.build().getDescriptorForType().findFieldByName(FIELD_NAME_VALUE);

            if (keyDescriptor == null) {
                throw new NotAvailableException("Field[KEY] does not exist for type " + entryMessageBuilder.getClass().getName());
            }

            if (valueDescriptor == null) {
                throw new NotAvailableException("Field[VALUE] does not exist for type " + entryMessageBuilder.getClass().getName());
            }

            entryMessageBuilder.setField(valueDescriptor, timestamp);
            entryMessageBuilder.setField(keyDescriptor, enumValueDescriptor);
            entryMessage = entryMessageBuilder.build();
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException | ClassCastException ex) {
            throw new CouldNotPerformException("Could not build service state entry!", ex);
        }

        updateValueOccurrence(entryMessage, serviceState);
    }

    /**
     * This method updates the occurrence of the given service state value.
     * <p>
     * Note: The entry will just replaced and no timestamp validation is performed.
     *
     * @param entryMessage the entry containing the service state value and the timestamp.
     * @param serviceState the service state message which holds the service value.
     *
     * @throws CouldNotPerformException is thrown if the update could not be performed.
     */
    public static void updateValueOccurrence(final Message entryMessage, final Message.Builder serviceState) throws CouldNotPerformException {
        final FieldDescriptor mapFieldDescriptor = serviceState.getDescriptorForType().findFieldByName(FIELD_NAME_LAST_VALUE_OCCURRENCE);
        if (mapFieldDescriptor == null) {
            throw new NotAvailableException("Field[last_value_occurrence] does not exist for type " + serviceState.getClass().getName());
        }
        ProtoBufFieldProcessor.putMapEntry(entryMessage, mapFieldDescriptor, serviceState);
    }

    /**
     * Returns the responsible action of the given service state.
     *
     * @param serviceState the service state that provides the responsible action
     *
     * @return the responsible action.
     *
     * @throws NotAvailableException in case the state dose not provide any responsible action or if the message is not a valid service state.
     */
    public static ActionDescription getResponsibleAction(final Message serviceState) throws NotAvailableException {
        final FieldDescriptor responsibleActionFieldDescriptor;

        // resolve field
        try {
            responsibleActionFieldDescriptor = ProtoBufFieldProcessor.getFieldDescriptor(serviceState, FIELD_NAME_RESPONSIBLE_ACTION);
        } catch (NotAvailableException ex) {
            new FatalImplementationErrorException("Given service state message does not provide the responsible action field!", serviceState, ex);
            throw new NotAvailableException("ActionDescription", ex);
        }

        // handle if responsible action is not set
        if (!serviceState.hasField(responsibleActionFieldDescriptor)) {
            throw new NotAvailableException("ActionDescription");
        }

        // resolve value
        return (ActionDescription) serviceState.getField(responsibleActionFieldDescriptor);
    }

    /**
     * Returns the responsible action builder of the given service state builder.
     *
     * @param serviceStateBuilder the service state builder that provides the responsible action builder.
     *
     * @return the responsible action builder.
     *
     * @throws NotAvailableException in case the state dose not provide any responsible action or if the builder is not a valid service state.
     */
    public static ActionDescription.Builder getResponsibleActionBuilder(final Message.Builder serviceStateBuilder) throws NotAvailableException {
        return ProtoBufBuilderProcessor.getBuilder(serviceStateBuilder, FIELD_NAME_RESPONSIBLE_ACTION, ActionDescription.Builder.class);
    }

    /**
     * Returns the action description of the given service state.
     *
     * @param serviceState the service state that provides the description.
     *
     * @return the responsible action.
     *
     * @throws NotAvailableException in case the state dose not provide any description action or if the message is not a valid service state.
     */
    public static MultiLanguageText getActionDescription(final Message serviceState) throws NotAvailableException {
        return ServiceStateProcessor.getResponsibleAction(serviceState).getDescription();
    }

    /**
     * Returns the responsible action of the given service state.
     *
     * @param serviceState the service state that provides the responsible action
     * @param alternative  a supplier that provides an alternative in case the action description is not available.
     *
     * @return the responsible action or its alternative.
     */
    public static ActionDescription getResponsibleAction(final Message serviceState, final Supplier<ActionDescription> alternative) {
        try {
            return getResponsibleAction(serviceState);
        } catch (NotAvailableException e) {
            return alternative.get();
        }
    }

    public static String getServiceValue(final MessageOrBuilder serviceState, final Supplier<String> alternative) {
        try {
            return getServiceValue(serviceState);
        } catch (NotAvailableException ex) {
            return alternative.get();
        }
    }

    /**
     * Method returns the value of the service state if available.
     * A value of PowerState could for example be "on".
     *
     * @param serviceState the service state used to derive the service value.
     *
     * @return the service value as string rep.
     *
     * @throws NotAvailableException is thrown in case the service value is not provided by the given service state.
     */
    public static String getServiceValue(final MessageOrBuilder serviceState) throws NotAvailableException {
        return ProtoBufFieldProcessor.getFieldValue(serviceState, "value").toString();
    }

    /**
     * Method computes the entire lower part of the action of the given service state modification. It can be used to precompute the impacted units without scheduling any actions.
     * Since the actions are not really executed and only precomputed, the returned list of action descriptions does not include the ids of the actions
     *
     * @param serviceStateDescription the description providing the service modification.
     *
     * @return a list of action descriptions that represent the impact of the service modification in case it would be applied.
     */
    public static Set<ActionDescription> computeActionImpact(final ServiceStateDescription serviceStateDescription) throws NotAvailableException {
        // check if registry is available
        Registries.getUnitRegistry();
        return computeActionImpact(serviceStateDescription, new HashSet<>());
    }

    private static Set<ActionDescription> computeActionImpact(final ServiceStateDescription serviceStateDescription, final HashSet<ServiceStateDescription> processedServiceStates) throws NotAvailableException {

        // handle termination:
        // make sure duplicated service states are only processed ones.
        if(processedServiceStates.contains(serviceStateDescription)) {
            return Collections.emptySet();
        } else {
            processedServiceStates.add(serviceStateDescription);
        }

        final HashSet<ActionDescription> actionImpactList = new HashSet<>();

        try {

            final UnitConfig impactedUnitConfig = Registries.getUnitRegistry().getUnitConfigById(serviceStateDescription.getUnitId());

            // in case a dal unit is referred this will be an end point and can directly be returned inclusive its influenced locations and groups
            if (UnitConfigProcessor.isDalUnit(impactedUnitConfig)) {

                // validate that service is compatible
                for (ServiceConfig serviceConfig : impactedUnitConfig.getServiceConfigList()) {
                    if (serviceConfig.getServiceDescription().getServiceType() == serviceStateDescription.getServiceType()) {
                        actionImpactList.add(buildActionImpact(serviceStateDescription).build());

                        // register all groups that are affected by this unit through service state aggregation
                        Registries.getUnitRegistry().getUnitConfigsByUnitType(UnitType.UNIT_GROUP)
                                .stream()
                                .filter(it -> it.getUnitGroupConfig().getMemberIdList().contains(impactedUnitConfig.getId()))
                                .forEach(it -> actionImpactList.add(
                                        buildActionImpact(
                                                serviceStateDescription.toBuilder().setUnitId(it.getId()).build()
                                        ).build()
                                ));

                        // register all locations that are affected by this unit through service state aggregation
                        Registries.getUnitRegistry().getUnitConfigsByUnitType(UnitType.LOCATION)
                                .stream()
                                .filter(it -> it.getLocationConfig().getUnitIdList().contains(impactedUnitConfig.getId()))
                                .forEach(it -> actionImpactList.add(
                                        buildActionImpact(
                                                serviceStateDescription.toBuilder().setUnitId(it.getId()).build()
                                        ).build()
                                ));
                        break;
                    }
                }
                return actionImpactList;
            }

            // for any base unit we have to collect its impact manually.
            switch (impactedUnitConfig.getUnitType()) {
                case LOCATION:
                    final List<UnitConfig> locationChildUnits = Registries.getUnitRegistry().getUnitConfigsByLocationIdAndUnitTypeInclusiveSuperTypeRecursive(impactedUnitConfig.getId(), serviceStateDescription.getUnitType(), true, false);
                    for (UnitConfig unitConfig : locationChildUnits) {

                        // all base units are not effected by location service operations except of child locations that might be affected through state aggregations.
                        if (UnitConfigProcessor.isBaseUnit(unitConfig) && unitConfig.getUnitType() != UnitType.LOCATION) {
                            continue;
                        }

                        actionImpactList.addAll(computeActionImpact(serviceStateDescription.toBuilder().setUnitId(unitConfig.getId()).build(), processedServiceStates));
                    }

                    // register location itself as impact in case it's affected by child units through aggregation
                    if(!locationChildUnits.isEmpty()) {
                        actionImpactList.add(buildActionImpact(serviceStateDescription).setIntermediary(true).build());
                    }
                    break;
                case UNIT_GROUP:
                    for (String memberId : impactedUnitConfig.getUnitGroupConfig().getMemberIdList()) {
                        actionImpactList.addAll(computeActionImpact(serviceStateDescription.toBuilder().setUnitId(memberId).build(), processedServiceStates));
                    }

                    // register group itself as impact
                    actionImpactList.add(buildActionImpact(serviceStateDescription).setIntermediary(true).build());
                    break;
                case SCENE:
                    // register required action impact
                    for (ServiceStateDescription impactedServiceStateDescription : impactedUnitConfig.getSceneConfig().getRequiredServiceStateDescriptionList()) {
                        actionImpactList.addAll(computeActionImpact(impactedServiceStateDescription, processedServiceStates));
                    }

                    // register optional action impact
                    for (ServiceStateDescription impactedServiceStateDescription : impactedUnitConfig.getSceneConfig().getOptionalServiceStateDescriptionList()) {
                        actionImpactList.addAll(computeActionImpact(impactedServiceStateDescription, processedServiceStates));
                    }

                    // register scene itself as impact
                    actionImpactList.add(buildActionImpact(serviceStateDescription).build());
                    break;
                case APP:
                case AGENT:
                    // register unit itself as impact
                    actionImpactList.add(buildActionImpact(serviceStateDescription).build());
                    break;
                case USER:
                case AUTHORIZATION_GROUP:
                case DEVICE:
                case GATEWAY:
                default:
                    // units do not have any impact on other units.
                    break;
            }

        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not compute action impact of " + serviceStateDescription.getUnitId(), ex, LoggerFactory.getLogger(UnitRegistry.class));
        }

        return actionImpactList;
    }

    private static ActionDescription.Builder buildActionImpact(final ServiceStateDescription serviceStateDescription) {
        return ActionDescription
                .newBuilder()
                .setActionId(Action.PRECOMPUTED_ACTION_ID)
                .setServiceStateDescription(serviceStateDescription);
    }

    public static String serializeServiceState(final Message.Builder serviceStateBuilder, final boolean verify) throws CouldNotPerformException {
        return JSON_PROCESSOR.serialize(verify ? Services.verifyAndRevalidateServiceState(serviceStateBuilder) : serviceStateBuilder);
    }

    public static String serializeServiceState(final Message serviceState, final boolean verify) throws CouldNotPerformException {
        return JSON_PROCESSOR.serialize(verify ? Services.verifyAndRevalidateServiceState(serviceState) : serviceState);
    }

    public static Message deserializeServiceState(final ServiceStateDescriptionOrBuilder serviceStateDescriptionOrBuilder) throws CouldNotPerformException {
        return deserializeServiceState(serviceStateDescriptionOrBuilder.getServiceState(), serviceStateDescriptionOrBuilder.getServiceStateClassName());
    }

    public static Message deserializeServiceState(final String serviceState, final String serviceStateClassName) throws CouldNotPerformException {
        return JSON_PROCESSOR.deserialize(serviceState, serviceStateClassName);
    }
}
