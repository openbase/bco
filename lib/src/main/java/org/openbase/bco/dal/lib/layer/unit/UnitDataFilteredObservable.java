package org.openbase.bco.dal.lib.layer.unit;

/*-
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
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
import com.google.protobuf.Message;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.pattern.AbstractObservable;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.type.domotic.service.ServiceDescriptionType.ServiceDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.service.ServiceTempusTypeType.ServiceTempusType.ServiceTempus;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 * @param <M>
 */
public class UnitDataFilteredObservable<M extends Message> extends AbstractObservable<DataProvider<M>, M> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnitDataFilteredObservable.class);

    private final DataProvider<M> unit;
    private final ServiceTempus serviceTempus;
    private final Set<String> fieldsToKeep;
    private UnitTemplate unitTemplate;

    public UnitDataFilteredObservable(final DataProvider<M> dataProvider, final ServiceTempus serviceTempus) {
        this(dataProvider, serviceTempus, null);
    }

    public UnitDataFilteredObservable(final DataProvider<M> dataProvider, final ServiceTempus serviceTempus, final UnitTemplate unitTemplate) {
        super(dataProvider);

        this.unit = dataProvider;
        this.serviceTempus = serviceTempus;
        this.setHashGenerator((M value) -> removeUnwantedServiceTempusAndIdFields(value.toBuilder()).build().hashCode());

        this.fieldsToKeep = new HashSet<>();
        this.unitTemplate = unitTemplate;
        if (unitTemplate != null) {
            updateFieldsToKeep();
        }
    }

    @Override
    public void waitForValue(long timeout, TimeUnit timeUnit) throws CouldNotPerformException, InterruptedException {
        unit.waitForData();
    }

    @Override
    public M getValue() throws NotAvailableException {
        return unit.getData();
    }

    @Override
    public boolean isValueAvailable() {
        return unit.isDataAvailable();
    }

    @Override
    public Future<M> getValueFuture() {
        return unit.getDataFuture();
    }

    private synchronized void updateFieldsToKeep() {
        if(serviceTempus == ServiceTempus.UNKNOWN) {
            return;
        }
        
        fieldsToKeep.clear();

        Set<ServiceType> serviceTypeSet = new HashSet<>();
        for (ServiceDescription serviceDescription : unitTemplate.getServiceDescriptionList()) {
            if (!serviceTypeSet.contains(serviceDescription.getServiceType())) {
                serviceTypeSet.add(serviceDescription.getServiceType());
                fieldsToKeep.add(Services.getServiceFieldName(serviceDescription.getServiceType(), serviceTempus));
            }
        }
    }

    public void updateToUnitTemplateChange(UnitTemplate unitTemplate) {
        this.unitTemplate = unitTemplate;
        updateFieldsToKeep();
    }

    private synchronized Message.Builder removeUnwantedServiceTempusAndIdFields(final Message.Builder builder) {
        // if unknown keep everything
        if(serviceTempus == ServiceTempus.UNKNOWN) {
            return builder;
        }

        // filter tempus
        Descriptors.Descriptor descriptorForType = builder.getDescriptorForType();
        descriptorForType.getFields().stream()
                .filter((field) -> (field.getType() == Descriptors.FieldDescriptor.Type.MESSAGE))
                .filter((field) -> (!fieldsToKeep.contains(field.getName()))).forEachOrdered((field) -> {
            builder.clearField(field);
        });
        LOGGER.trace("For {} let pass Fields[{}]: {}", serviceTempus.name(), fieldsToKeep.toString(), builder.build());
        return builder;
    }
}
