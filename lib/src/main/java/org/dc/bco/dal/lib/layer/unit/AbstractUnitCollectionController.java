/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.lib.layer.unit;

/*
 * #%L
 * DAL Library
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
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
import com.google.protobuf.GeneratedMessage;

import java.util.HashMap;
import java.util.Map;
import org.dc.bco.dal.lib.layer.unit.AbstractUnitController;
import org.dc.bco.dal.lib.transform.UnitConfigToUnitClassTransformer;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.CouldNotTransformException;
import org.dc.jul.extension.rsb.com.RSBCommunicationService;
import java.util.Collection;
import java.util.Collections;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.exception.VerificationFailedException;
import org.dc.jul.extension.protobuf.ClosableDataBuilder;
import org.dc.jul.extension.rsb.iface.RSBLocalServerInterface;
import org.dc.jul.iface.Identifiable;
import org.dc.jul.processing.StringProcessor;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import rst.homeautomation.unit.UnitConfigType;

/**
 *
 * @author Divine Threepwood
 * @param <M> Underling message type.
 * @param <MB> Message related builder.
 */
public abstract class AbstractUnitCollectionController<M extends GeneratedMessage, MB extends M.Builder<MB>> extends RSBCommunicationService<M, MB> implements Identifiable<String>, UnitHost {

    private final Map<String, AbstractUnitController> unitMap;

    public AbstractUnitCollectionController(final MB builder) throws InstantiationException {
        super(builder);
        this.unitMap = new HashMap<>();
    }

    public final static String generateName(final Class hardware) {
        return hardware.getSimpleName().replace("Controller", "");
    }

    protected <U extends AbstractUnitController> void registerUnit(final U unit) throws VerificationFailedException {
        if (unitMap.containsKey(unit.getId())) {
            throw new VerificationFailedException("Could not register " + unit + "! Unit with same name already registered!");
        }
        unitMap.put(unit.getId(), unit);
    }

    public AbstractUnitController getUnit(final String id) throws NotAvailableException {
        if (!unitMap.containsKey(id)) {
            throw new NotAvailableException("Unit[" + id + "]", this + " has no registered unit with given name!");
        }
        return unitMap.get(id);
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        super.activate();

        for (AbstractUnitController unit : unitMap.values()) {
            unit.activate();
        }
    }

    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        super.deactivate();

        for (AbstractUnitController unit : unitMap.values()) {
            unit.deactivate();
        }
    }

    @Override
    public void registerMethods(RSBLocalServerInterface server) {
        // dummy construct: For registering methods overwrite this method.
    }

    @Override
    public String toString() {
        try {
            return getClass().getSimpleName() + "[" + getId() + "]";
        } catch (CouldNotPerformException ex) {
            return getClass().getSimpleName() + "[?]";
        }
    }

    public final void registerUnits(final Collection<UnitConfigType.UnitConfig> unitConfigs) throws CouldNotPerformException {
        for (UnitConfigType.UnitConfig unitConfig : unitConfigs) {
            registerUnit(unitConfig);
        }
    }

    public final void registerUnit(final UnitConfigType.UnitConfig unitConfig) throws CouldNotPerformException {
        try {
            GeneratedMessage.Builder unitMessageBuilder = registerUnitBuilder(unitConfig);

            Constructor<? extends AbstractUnitController> unitConstructor;
            try {
                unitConstructor = UnitConfigToUnitClassTransformer.transform(unitConfig).getConstructor(UnitConfigType.UnitConfig.class, UnitHost.class, unitMessageBuilder.getClass());
            } catch (CouldNotTransformException | NoSuchMethodException | SecurityException | NullPointerException ex) {
                throw new CouldNotPerformException("Could not instantiate Unit[" + unitConfig + "]!", ex);
            }
            AbstractUnitController unit;
            try {
                unit = unitConstructor.newInstance(unitConfig, this, unitMessageBuilder);
            } catch (java.lang.InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NullPointerException ex) {
                throw new CouldNotPerformException("Could not instantiate Unit[" + unitConfig + "]!", ex);
            }
            registerUnit(unit);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not register Unit[" + unitConfig + "]!", ex);
        }
    }

    private <B extends GeneratedMessage.Builder> B registerUnitBuilder(final UnitConfigType.UnitConfig unitConfig) throws CouldNotPerformException {
        try (ClosableDataBuilder<MB> dataBuilder = getDataBuilder(this)) {
            MB builder = dataBuilder.getInternalBuilder();
            Class builderClass = builder.getClass();
            String unitTypeName = StringProcessor.transformUpperCaseToCamelCase(unitConfig.getType().name());
            String repeatedUnitFieldName = "unit_" + unitConfig.getType().name().toLowerCase();
            Descriptors.FieldDescriptor repeatedUnitFieldDescriptor = builder.getDescriptorForType().findFieldByName(repeatedUnitFieldName);

            if (repeatedUnitFieldDescriptor == null) {
                throw new CouldNotPerformException("Missing FieldDescriptor[" + repeatedUnitFieldName + "] in protobuf Type[" + builder.getClass().getName() + "]!");
            }

            GeneratedMessage.Builder unitBuilder = loadUnitBuilder(unitConfig);
            Method addUnitMethod;
            try {
                addUnitMethod = builderClass.getMethod("addUnit" + unitTypeName, unitBuilder.getClass());
            } catch (Exception ex) {
                throw new CouldNotPerformException("Missing repeated field for " + unitBuilder.getClass().getName() + " in protobuf Type[" + builder.getClass().getName() + "]! ", ex);
            }

            try {
                addUnitMethod.invoke(builder, unitBuilder);
            } catch (Exception ex) {
                throw new CouldNotPerformException("Could not add " + unitBuilder.getClass().getName() + " to message of Type[" + builder.getClass().getName() + "]! ", ex);
            }

            try {
                return (B) builderClass.getMethod("getUnit" + unitTypeName + "Builder", int.class).invoke(builder, builder.getRepeatedFieldCount(repeatedUnitFieldDescriptor) - 1);
            } catch (Exception ex) {
                throw new CouldNotPerformException("Could not create Builder!", ex);
            }

        } catch (Exception ex) {
            throw new CouldNotPerformException("Could register UnitBuilder[" + unitConfig + "]!", ex);
        }
    }

    private GeneratedMessage.Builder loadUnitBuilder(final UnitConfigType.UnitConfig unitConfig) throws CouldNotPerformException {
        GeneratedMessage.Builder builder = null;
        try {
            String unitTypeName = StringProcessor.transformUpperCaseToCamelCase(unitConfig.getType().name());
            String unitMessageClassName = "rst.homeautomation.unit." + unitTypeName + "Type$" + unitTypeName;
            Class messageClass;
            try {
                messageClass = Class.forName(unitMessageClassName);
            } catch (ClassNotFoundException ex) {
                throw new CouldNotPerformException("Could not find builder Class[" + unitMessageClassName + "]!", ex);
            }

            try {
                builder = (GeneratedMessage.Builder) messageClass.getMethod("newBuilder", null).invoke(null, null);
            } catch (NoSuchMethodException | SecurityException ex) {
                throw new CouldNotPerformException("Could not instantiate builder out of Class[" + messageClass.getName() + "]!", ex);
            }

        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not load builder for " + unitConfig.getId() + "!", ex);
        }
        return builder;
    }

    public Collection<AbstractUnitController> getUnits() {
        return Collections.unmodifiableCollection(unitMap.values());
    }
}
