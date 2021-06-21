package org.openbase.bco.registry.unit.core.consistency.sceneconfig;

/*-
 * #%L
 * BCO Registry Unit Core
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import org.openbase.bco.registry.template.remote.CachedTemplateRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.extension.protobuf.processing.ProtoBufJSonProcessor;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.type.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import org.openbase.type.domotic.service.ServiceDescriptionType.ServiceDescription;
import org.openbase.type.domotic.service.ServiceStateDescriptionType.ServiceStateDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig.Builder;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate;
import org.openbase.type.domotic.unit.scene.SceneConfigType.SceneConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Consistency handler validating service state descriptions inside a service config.
 * For a detailed list of changes performed see {@link #validateServiceStateDescriptions(SceneConfig.Builder, FieldDescriptor)}.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class SceneServiceStateConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, Builder> {

    private final List<ProtoBufFileSynchronizedRegistry<String, UnitConfig, Builder, UnitRegistryData.Builder>> unitRegistryList;
    private final ProtoBufJSonProcessor protoBufJSonProcessor;

    private final FieldDescriptor requiredServiceStateDescriptionField;
    private final FieldDescriptor optionalServiceStateDescriptionField;

    /**
     * {@inheritDoc}
     *
     * @param unitRegistryList a list of all unit registries. Needed to verify if the unit id in the service state
     *                         description exists.
     */
    public SceneServiceStateConsistencyHandler(final List<ProtoBufFileSynchronizedRegistry<String, UnitConfig, Builder, UnitRegistryData.Builder>> unitRegistryList) {
        this.unitRegistryList = unitRegistryList;

        this.protoBufJSonProcessor = new ProtoBufJSonProcessor();
        this.requiredServiceStateDescriptionField = SceneConfig.getDescriptor().findFieldByNumber(SceneConfig.REQUIRED_SERVICE_STATE_DESCRIPTION_FIELD_NUMBER);
        this.optionalServiceStateDescriptionField = SceneConfig.getDescriptor().findFieldByNumber(SceneConfig.OPTIONAL_SERVICE_STATE_DESCRIPTION_FIELD_NUMBER);
    }

    /**
     * {@inheritDoc}
     *
     * @param id       {@inheritDoc}
     * @param entry    {@inheritDoc}
     * @param entryMap {@inheritDoc}
     * @param registry {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     * @throws EntryModification        {@inheritDoc}
     */
    @Override
    public void processData(final String id, final IdentifiableMessage<String, UnitConfig, Builder> entry,
                            final ProtoBufMessageMap<String, UnitConfig, Builder> entryMap,
                            final ProtoBufRegistry<String, UnitConfig, Builder> registry) throws CouldNotPerformException, EntryModification {
        final UnitConfig.Builder sceneUnitConfig = entry.getMessage().toBuilder();
        final SceneConfig.Builder sceneConfig = sceneUnitConfig.getSceneConfigBuilder();

        if (validateServiceStateDescriptions(sceneConfig, requiredServiceStateDescriptionField) ||
                validateServiceStateDescriptions(sceneConfig, optionalServiceStateDescriptionField)) {
            throw new EntryModification(entry.setMessage(sceneUnitConfig.build(), this), this);
        }
    }

    /**
     * Validate a list of service state descriptions inside a scene config. Service state descriptions are removed
     * if the defined unit does not exists, does not have the defined service or the service attribute could not
     * be de-serialized. The unit type in the service state description is updated if it does not match the type
     * of the defined unit and the service attribute type is updated if it does not match the defined service type.
     *
     * @param sceneConfig     the scene config in which a list of service state description is updated.
     * @param fieldDescriptor field descriptor of a field containing a list of service state descriptions. This is
     *                        either the required or optional service state description field.
     *
     * @return if a modification to the service state description list occurred.
     *
     * @throws CouldNotPerformException if the validation fails.
     */
    @SuppressWarnings("unchecked")
    private boolean validateServiceStateDescriptions(final SceneConfig.Builder sceneConfig, final FieldDescriptor fieldDescriptor) throws CouldNotPerformException {
        boolean modification = false;
        final List<ServiceStateDescription> serviceStateDescriptionList = new ArrayList<>((List<ServiceStateDescription>) sceneConfig.getField(fieldDescriptor));
        sceneConfig.clearField(fieldDescriptor);
        for (final ServiceStateDescription serviceStateDescription : serviceStateDescriptionList) {
            final ServiceStateDescription.Builder builder = serviceStateDescription.toBuilder();
            UnitConfig unitConfig = null;
            for (final ProtoBufFileSynchronizedRegistry<String, UnitConfig, Builder, UnitRegistryData.Builder> unitRegistry : unitRegistryList) {
                if (unitRegistry.contains(builder.getUnitId())) {
                    unitConfig = unitRegistry.getMessage(builder.getUnitId());
                    break;
                }
            }

            if (unitConfig == null) {
                logger.debug("Remove serviceStateDescription of unit {} because it does not exist", builder.getUnitId());
                modification = true;
                continue;
            }

            boolean validServiceTypeForUnit = false;
            final UnitTemplate unitTemplate = CachedTemplateRegistryRemote.getRegistry().getUnitTemplateByType(unitConfig.getUnitType());
            for (final ServiceDescription serviceDescription : unitTemplate.getServiceDescriptionList()) {
                if (serviceDescription.getPattern() != ServicePattern.OPERATION) {
                    continue;
                }

                if (serviceDescription.getServiceType() == serviceDescription.getServiceType()) {
                    validServiceTypeForUnit = true;
                    break;
                }
            }

            if (!validServiceTypeForUnit) {
                logger.debug("Remove serviceStateDescription because serviceType {} is not an operation service of unitType {}",
                        builder.getServiceType().name(), builder.getUnitType().name());
                modification = true;
                continue;
            }

            final String serviceStateClassName = CachedTemplateRegistryRemote.getRegistry().getServiceStateClassName(builder.getServiceType());
            if (!builder.getServiceStateClassName().equals(serviceStateClassName)) {
                logger.debug("Update serviceStateClassName of serviceStateDescription from {} to {}", builder.getServiceStateClassName(), serviceStateClassName);
                builder.setServiceStateClassName(serviceStateClassName);
                modification = true;
            }

            try {
                // try to deserialize
                final Message serviceState = protoBufJSonProcessor.deserialize(builder.getServiceState(), builder.getServiceStateClassName());

                // reformat if necessary
                final String serviceStateString = protoBufJSonProcessor.serialize(serviceState);
                if (!serviceStateString.equals(builder.getServiceState())) {
                    builder.setServiceState(serviceStateString);
                    modification = true;
                }

            } catch (CouldNotPerformException ex) {
                logger.debug("Remove serviceStateDescription because the attribute {} could not be de-serialized into attributeType {}", builder.getServiceStateClassName());
                modification = true;
                continue;
            }

            sceneConfig.addRepeatedField(fieldDescriptor, builder.build());
        }
        return modification;
    }
}
