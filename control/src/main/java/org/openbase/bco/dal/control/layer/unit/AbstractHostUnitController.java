package org.openbase.bco.dal.control.layer.unit;

/*
 * #%L
 * BCO DAL Control
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

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import org.openbase.bco.dal.lib.layer.unit.HostUnitController;
import org.openbase.bco.dal.lib.layer.unit.UnitController;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.*;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
import org.openbase.jul.extension.protobuf.ProtobufListDiff;
import org.openbase.jul.extension.protobuf.iface.DataBuilderProvider;
import org.openbase.jul.extension.type.processing.ScopeProcessor;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.schedule.CloseableWriteLockWrapper;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @param <D>  the data type of this unit used for the state synchronization.
 * @param <DB> the builder used to build the unit data instance.
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public abstract class AbstractHostUnitController<D extends AbstractMessage & Serializable, DB extends D.Builder<DB>, C extends UnitController<?, ?>> extends AbstractBaseUnitController<D, DB> implements HostUnitController<D, DB, C> {

    //TODO: use a unit controller synchronizer instead of the combination of a diff and unit map
    private final Map<String, C> unitMap;
    private final ProtobufListDiff<String, UnitConfig, UnitConfig.Builder> hostedUnitDiff;
    private final SyncObject unitMapLock = new SyncObject("UnitMapLock");

    public AbstractHostUnitController(final DB builder) throws InstantiationException {
        super(builder);
        this.unitMap = new HashMap<>();
        this.hostedUnitDiff = new ProtobufListDiff<>();
    }

    private void registerUnitController(final C unit) throws CouldNotPerformException {
        synchronized (unitMapLock) {
            try {
                if (unitMap.containsKey(unit.getId())) {
                    throw new VerificationFailedException("Could not register " + unit + "! Unit with same name already registered!");
                }
                unitMap.put(unit.getId(), unit);
            } catch (CouldNotPerformException ex) {
                throw new CouldNotPerformException("Could not registerUnit!", ex);
            }
        }
    }

    @Override
    public C getHostedUnitController(final String id) throws NotAvailableException {
        synchronized (unitMapLock) {
            if (!unitMap.containsKey(id)) {
                throw new NotAvailableException("Unit[" + id + "]", this + " has no registered unit with given name!");
            }
            return unitMap.get(id);
        }
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        super.activate();
        MultiException.ExceptionStack exceptionStack = null;
        synchronized (unitMapLock) {
            for (UnitController unit : unitMap.values()) {
                try {
                    unit.activate();
                } catch (CouldNotPerformException ex) {
                    exceptionStack = MultiException.push(this, ex, exceptionStack);
                }
            }
        }
        MultiException.checkAndThrow(() -> "Could not activate all hosted units of " + this, exceptionStack);
    }

    @Override
    public void shutdown() {
        super.shutdown();
        synchronized (unitMapLock) {
            for (UnitController<?, ?> unit : unitMap.values()) {
                unit.shutdown();
            }
        }
    }

    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        super.deactivate();
        MultiException.ExceptionStack exceptionStack = null;
        synchronized (unitMapLock) {
            for (UnitController unit : unitMap.values()) {
                try {
                    unit.deactivate();
                } catch (CouldNotPerformException ex) {
                    exceptionStack = MultiException.push(this, ex, exceptionStack);
                }
            }
        }
        MultiException.checkAndThrow(() -> "Could not deactivate all hosted units of " + this, exceptionStack);
    }

    @Override
    public UnitConfig applyConfigUpdate(UnitConfig config) throws CouldNotPerformException, InterruptedException {
        try (final CloseableWriteLockWrapper ignored = getManageWriteLockInterruptible(this)) {
            UnitConfig unitConfig = super.applyConfigUpdate(config);
            Registries.waitForData();

            try {
                synchronized (unitMapLock) {
                    hostedUnitDiff.diffMessages(getHostedUnitConfigList());
                    MultiException.ExceptionStack removeExceptionStack = null;
                    hostedUnitDiff.getRemovedMessageMap().getMessages().forEach((removedUnitConfig) -> {
                        unitMap.remove(removedUnitConfig.getId()).shutdown();
                    });

                    /*
                     * unitController handle their update themselves
                     */
                    MultiException.ExceptionStack registerExceptionStack = null;
                    for (UnitConfig newUnitConfig : hostedUnitDiff.getNewMessageMap().getMessages()) {
                        try {
                            registerUnit(newUnitConfig);
                        } catch (CouldNotPerformException ex) {
                            registerExceptionStack = MultiException.push(this, ex, registerExceptionStack);
                        }
                    }

                    MultiException.ExceptionStack exceptionStack = null;
                    int counter;
                    try {
                        counter = 0;
                        final int internalCounter = counter;
                        MultiException.checkAndThrow(() -> "Could not remove " + internalCounter + " unitController!", removeExceptionStack);
                    } catch (CouldNotPerformException ex) {
                        exceptionStack = MultiException.push(this, ex, exceptionStack);
                    }
                    try {
                        if (registerExceptionStack != null) {
                            counter = registerExceptionStack.size();
                        } else {
                            counter = 0;
                        }
                        final int internalCounter = counter;
                        MultiException.checkAndThrow(() -> "Could not register " + internalCounter + " unitController!", registerExceptionStack);
                    } catch (CouldNotPerformException ex) {
                        exceptionStack = MultiException.push(this, ex, exceptionStack);
                    }
                    MultiException.checkAndThrow(() -> "Could not update unitHostController!", exceptionStack);
                }
            } catch (CouldNotPerformException ex) {
                throw new CouldNotPerformException("Could not applyConfigUpdate for UnitHost[" + ScopeProcessor.generateStringRep(unitConfig.getScope()) + "]", ex);
            }

            return unitConfig;
        }
    }

    protected Set<String> getRemovedUnitIds() {
        Set<String> removedUnitIds = new HashSet<>();
        hostedUnitDiff.getRemovedMessageMap().getMessages().forEach((removedUnitConfig) -> {
            removedUnitIds.add(removedUnitConfig.getId());
        });
        return removedUnitIds;
    }

    protected Set<C> getNewUnitController() {
        Set<C> newUnitController = new HashSet<>();
        hostedUnitDiff.getNewMessageMap().getMessages().forEach((newUnitConfig) -> {
            newUnitController.add(unitMap.get(newUnitConfig.getId()));
        });
        return newUnitController;
    }

    private final void registerUnits(final Collection<UnitConfig> unitConfigs) throws CouldNotPerformException, InterruptedException {
        MultiException.ExceptionStack exceptionStack = null;
        for (final UnitConfig unitConfig : unitConfigs) {
            try {
                registerUnit(unitConfig);
            } catch (CouldNotPerformException ex) {
                exceptionStack = MultiException.push(this, ex, exceptionStack);
            }
        }
        MultiException.checkAndThrow(() -> "Could not register all hosted units of " + this, exceptionStack);
    }

    private final void registerUnit(final UnitConfig unitConfig) throws CouldNotPerformException, InterruptedException {
        final C unitController = buildUnitController(unitConfig);
        if (isActive()) {
            unitController.activate();
        }
        registerUnitController(unitController);
    }

    @Override
    public List<C> getHostedUnitControllerList() {
        return new ArrayList<>(unitMap.values());
    }

    protected abstract C buildUnitController(final UnitConfig unitConfig) throws CouldNotPerformException, InterruptedException;

    public static class DALUnitFactory<D extends AbstractMessage & Serializable, DB extends D.Builder<DB>, C extends UnitController<?, ?>> {

        public C newInstance(final UnitConfig unitConfig, final AbstractHostUnitController<D, DB, C> dataBuilderProvider) throws CouldNotPerformException, InterruptedException {
            try {
                final Message.Builder unitMessageBuilder = resolveUnitBuilder(unitConfig, dataBuilderProvider);
                final Constructor<C> unitConstructor;
                try {
                    unitConstructor = (Constructor<C>) detectUnitControllerClass(unitConfig).getConstructor(HostUnitController.class, unitMessageBuilder.getClass());
                } catch (CouldNotTransformException | NoSuchMethodException | SecurityException | NullPointerException ex) {
                    throw new CouldNotPerformException("Could not instantiate Unit[" + ScopeProcessor.generateStringRep(unitConfig.getScope()) + "]!", ex);
                }
                C unit;
                try {
                    unit = unitConstructor.newInstance(dataBuilderProvider, unitMessageBuilder);
                } catch (java.lang.InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NullPointerException ex) {
                    throw new CouldNotPerformException("Could not instantiate Unit[" + ScopeProcessor.generateStringRep(unitConfig.getScope()) + "]!", ex);
                }
                unit.init(unitConfig);
                return unit;
            } catch (CouldNotPerformException ex) {
                throw new CouldNotPerformException("Could not register Unit[" + ScopeProcessor.generateStringRep(unitConfig.getScope()) + "]!", ex);
            }
        }

        public <B extends Message.Builder> B resolveUnitBuilder(final UnitConfig unitConfig, final DataBuilderProvider<D, DB> dataBuilderProvider) throws CouldNotPerformException, InterruptedException {
            try (ClosableDataBuilder<DB> dataBuilder = dataBuilderProvider.getDataBuilderInterruptible(this)) {
                final DB builder = dataBuilder.getInternalBuilder();
                final Class builderClass = builder.getClass();
                final String unitTypeName = StringProcessor.transformUpperCaseToPascalCase(unitConfig.getUnitType().name());
                final String repeatedUnitFieldName = "unit_" + unitConfig.getUnitType().name().toLowerCase() + "_data";
                final Descriptors.FieldDescriptor repeatedUnitFieldDescriptor = builder.getDescriptorForType().findFieldByName(repeatedUnitFieldName);

                if (repeatedUnitFieldDescriptor == null) {
                    throw new CouldNotPerformException("Missing FieldDescriptor[" + repeatedUnitFieldName + "] in protobuf Type[" + builder.getClass().getName() + "]!");
                }

                Message.Builder unitBuilder = UnitConfigProcessor.generateUnitDataBuilder(unitConfig);
                Method addUnitMethod;
                try {
                    addUnitMethod = builderClass.getMethod("addUnit" + unitTypeName + "Data", unitBuilder.getClass());
                } catch (Exception ex) {
                    throw new CouldNotPerformException("Missing repeated field for " + unitBuilder.getClass().getName() + " in protobuf Type[" + builder.getClass().getName() + "]! ", ex);
                }

                try {
                    addUnitMethod.invoke(builder, unitBuilder);
                } catch (Exception ex) {
                    throw new CouldNotPerformException("Could not add " + unitBuilder.getClass().getName() + " to message of Type[" + builder.getClass().getName() + "]! ", ex);
                }

                try {
                    return (B) builderClass.getMethod("getUnit" + unitTypeName + "DataBuilder", int.class).invoke(builder, builder.getRepeatedFieldCount(repeatedUnitFieldDescriptor) - 1);
                } catch (Exception ex) {
                    throw new CouldNotPerformException("Could not create Builder!", ex);
                }

            } catch (CouldNotPerformException ex) {
                throw new CouldNotPerformException("Could register UnitBuilder[" + ScopeProcessor.generateStringRep(unitConfig.getScope()) + "]!", ex);
            }
        }
    }
}
