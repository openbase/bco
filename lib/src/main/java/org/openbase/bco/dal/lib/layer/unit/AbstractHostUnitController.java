package org.openbase.bco.dal.lib.layer.unit;

/*
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openbase.bco.dal.lib.layer.service.ServiceFactoryProvider;
import org.openbase.bco.dal.lib.transform.UnitConfigToUnitClassTransformer;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.remote.CachedUnitRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.CouldNotTransformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.MultiException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
import org.openbase.jul.extension.protobuf.ProtobufListDiff;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.schedule.SyncObject;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 *
 * @param <D> the data type of this unit used for the state synchronization.
 * @param <DB> the builder used to build the unit data instance.
 */
public abstract class AbstractHostUnitController<D extends GeneratedMessage, DB extends D.Builder<DB>> extends AbstractBaseUnitController<D, DB> implements UnitHost<D>, ServiceFactoryProvider {

    private final Map<String, AbstractUnitController<?, ?>> unitMap;
    private final ProtobufListDiff<String, UnitConfig, UnitConfig.Builder> hostedUnitDiff;
    private final SyncObject unitMapLock = new SyncObject("UnitMapLock");

    public AbstractHostUnitController(final Class unitClass, final DB builder) throws InstantiationException {
        super(unitClass, builder);
        this.unitMap = new HashMap<>();
        this.hostedUnitDiff = new ProtobufListDiff<>();
    }

    protected abstract List<UnitConfig> getHostedUnits() throws NotAvailableException, InterruptedException;

    protected <U extends AbstractUnitController<?, ?>> void registerUnit(final U unit) throws CouldNotPerformException {
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

    public AbstractUnitController<?, ?> getHostedUnitController(final String id) throws NotAvailableException {
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
            for (AbstractUnitController unit : unitMap.values()) {
                try {
                    unit.activate();
                } catch (CouldNotPerformException ex) {
                    exceptionStack = MultiException.push(this, ex, exceptionStack);
                }
            }
        }
        MultiException.checkAndThrow("Could not activate all hosted units of " + this, exceptionStack);
    }

    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        super.deactivate();
        MultiException.ExceptionStack exceptionStack = null;
        synchronized (unitMapLock) {
            for (AbstractUnitController unit : unitMap.values()) {
                try {
                    unit.deactivate();
                } catch (CouldNotPerformException ex) {
                    exceptionStack = MultiException.push(this, ex, exceptionStack);
                }
            }
        }
        MultiException.checkAndThrow("Could not deactivate all hosted units of " + this, exceptionStack);
    }

    @Override
    public UnitConfig applyConfigUpdate(UnitConfig config) throws CouldNotPerformException, InterruptedException {
        UnitConfig unitConfig = super.applyConfigUpdate(config);
        Registries.getUnitRegistry().waitForData();

        try {
            synchronized (unitMapLock) {
                hostedUnitDiff.diff(getHostedUnits());
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
                    if (removeExceptionStack != null) {
                        counter = removeExceptionStack.size();
                    } else {
                        counter = 0;
                    }
                    MultiException.checkAndThrow("Could not remove " + counter + " unitController!", removeExceptionStack);
                } catch (CouldNotPerformException ex) {
                    exceptionStack = MultiException.push(this, ex, exceptionStack);
                }
                try {
                    if (registerExceptionStack != null) {
                        counter = registerExceptionStack.size();
                    } else {
                        counter = 0;
                    }
                    MultiException.checkAndThrow("Could not register " + counter + " unitController!", registerExceptionStack);
                } catch (CouldNotPerformException ex) {
                    exceptionStack = MultiException.push(this, ex, exceptionStack);
                }
                MultiException.checkAndThrow("Could not update unitHostController!", exceptionStack);
            }
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not applyConfigUpdate for UnitHost[" + ScopeGenerator.generateStringRep(unitConfig.getScope()) + "]", ex);
        }

        return unitConfig;
    }

    protected Set<String> getRemovedUnitIds() {
        Set<String> removedUnitIds = new HashSet<>();
        hostedUnitDiff.getRemovedMessageMap().getMessages().forEach((removedUnitConfig) -> {
            removedUnitIds.add(removedUnitConfig.getId());
        });
        return removedUnitIds;
    }

    protected Set<AbstractUnitController> getNewUnitController() {
        Set<AbstractUnitController> newUnitController = new HashSet<>();
        hostedUnitDiff.getNewMessageMap().getMessages().forEach((newUnitConfig) -> {
            newUnitController.add(unitMap.get(newUnitConfig.getId()));
        });
        return newUnitController;
    }

    public final void registerUnits(final Collection<UnitConfig> unitConfigs) throws CouldNotPerformException, InterruptedException {
        MultiException.ExceptionStack exceptionStack = null;
        for (UnitConfig unitConfig : unitConfigs) {
            try {
                registerUnit(unitConfig);
            } catch (CouldNotPerformException ex) {
                exceptionStack = MultiException.push(this, ex, exceptionStack);
            }
        }
        MultiException.checkAndThrow("Could not register all hosted units of " + this, exceptionStack);
    }

    public final void registerUnitsById(final Collection<String> unitIds) throws CouldNotPerformException, InterruptedException {
        CachedUnitRegistryRemote.waitForData();
        MultiException.ExceptionStack exceptionStack = null;

        for (String unitId : unitIds) {
            try {
                registerUnit(Registries.getUnitRegistry(true).getUnitConfigById(unitId));
            } catch (CouldNotPerformException ex) {
                exceptionStack = MultiException.push(this, ex, exceptionStack);
            }
        }
        MultiException.checkAndThrow("Could not register all hosted units of " + this, exceptionStack);
    }

    //TODO mpohling: implement unit factory instead!
    public final void registerUnit(final UnitConfig unitConfig) throws CouldNotPerformException, InitializationException, InterruptedException {
        try {
            GeneratedMessage.Builder unitMessageBuilder = registerUnitBuilder(unitConfig);

            Constructor<? extends AbstractUnitController> unitConstructor;
            try {
                unitConstructor = UnitConfigToUnitClassTransformer.transform(unitConfig).getConstructor(UnitHost.class, unitMessageBuilder.getClass());
            } catch (CouldNotTransformException | NoSuchMethodException | SecurityException | NullPointerException ex) {
                throw new CouldNotPerformException("Could not instantiate Unit[" + ScopeGenerator.generateStringRep(unitConfig.getScope()) + "]!", ex);
            }
            AbstractUnitController unit;
            try {
                unit = unitConstructor.newInstance(this, unitMessageBuilder);
            } catch (java.lang.InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NullPointerException ex) {
                throw new CouldNotPerformException("Could not instantiate Unit[" + ScopeGenerator.generateStringRep(unitConfig.getScope()) + "]!", ex);
            }
            unit.init(unitConfig);
            registerUnit(unit);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not register Unit[" + ScopeGenerator.generateStringRep(unitConfig.getScope()) + "]!", ex);
        }
    }

    private <B extends GeneratedMessage.Builder> B registerUnitBuilder(final UnitConfig unitConfig) throws CouldNotPerformException {
        try (ClosableDataBuilder<DB> dataBuilder = getDataBuilder(this, isActive())) {
            DB builder = dataBuilder.getInternalBuilder();
            Class builderClass = builder.getClass();
            String unitTypeName = StringProcessor.transformUpperCaseToCamelCase(unitConfig.getType().name());
            String repeatedUnitFieldName = "unit_" + unitConfig.getType().name().toLowerCase() + "_data";
            Descriptors.FieldDescriptor repeatedUnitFieldDescriptor = builder.getDescriptorForType().findFieldByName(repeatedUnitFieldName);

            if (repeatedUnitFieldDescriptor == null) {
                throw new CouldNotPerformException("Missing FieldDescriptor[" + repeatedUnitFieldName + "] in protobuf Type[" + builder.getClass().getName() + "]!");
            }

            GeneratedMessage.Builder unitBuilder = loadUnitBuilder(unitConfig);
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
            throw new CouldNotPerformException("Could register UnitBuilder[" + ScopeGenerator.generateStringRep(unitConfig.getScope()) + "]!", ex);
        }
    }

    private GeneratedMessage.Builder loadUnitBuilder(final UnitConfig unitConfig) throws CouldNotPerformException {
        GeneratedMessage.Builder builder = null;
        try {
            String unitTypeName = StringProcessor.transformUpperCaseToCamelCase(unitConfig.getType().name());
            String unitMessageClassName = "rst.domotic.unit.dal." + unitTypeName + "DataType$" + unitTypeName + "Data";
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
            throw new CouldNotPerformException("Could not load builder for " + ScopeGenerator.generateStringRep(unitConfig.getScope()) + "!", ex);
        }
        return builder;
    }

    public Collection<AbstractUnitController> getHostedUnitController() {
        return Collections.unmodifiableCollection(unitMap.values());
    }
}
