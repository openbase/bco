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
import java.util.Map;
import org.openbase.bco.dal.lib.transform.UnitConfigToUnitClassTransformer;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.bco.registry.unit.remote.CachedUnitRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.CouldNotTransformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
import org.openbase.jul.extension.rsb.com.AbstractConfigurableController;
import org.openbase.jul.extension.rsb.iface.RSBLocalServer;
import org.openbase.jul.iface.Identifiable;
import org.openbase.jul.processing.StringProcessor;
import rst.domotic.unit.UnitConfigType;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 * @param <M> Underling message type.
 * @param <MB> Message related builder.
 * @param <CONFIG>
 */
public abstract class AbstractHostUnitController<M extends GeneratedMessage, MB extends M.Builder<MB>, CONFIG extends GeneratedMessage> extends AbstractConfigurableController<M, MB, CONFIG> implements Identifiable<String>, UnitHost {

    private final Map<String, AbstractUnitController> unitMap;
    private UnitRegistry unitRegistry;

    public AbstractHostUnitController(final MB builder) throws InstantiationException {
        super(builder);
        this.unitMap = new HashMap<>();
    }

    public final static String generateName(final Class hardware) {
        return hardware.getSimpleName().replace("Controller", "");
    }

    protected <U extends AbstractUnitController> void registerUnit(final U unit) throws CouldNotPerformException {
        try {
            if (unitMap.containsKey(unit.getId())) {
                throw new VerificationFailedException("Could not register " + unit + "! Unit with same name already registered!");
            }
            unitMap.put(unit.getId(), unit);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not registerUnit!", ex);
        }
    }

    public AbstractUnitController getUnit(final String id) throws NotAvailableException {
        if (!unitMap.containsKey(id)) {
            throw new NotAvailableException("Unit[" + id + "]", this + " has no registered unit with given name!");
        }
        return unitMap.get(id);
    }

    @Override
    protected void postInit() throws InitializationException, InterruptedException {
        super.postInit();
        try {
            this.unitRegistry = CachedUnitRegistryRemote.getRegistry();
        } catch (NotAvailableException ex) {
            throw new InitializationException(this, ex);
        }
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
    public void registerMethods(RSBLocalServer server) {
        // dummy construct: For registering methods overwrite this method.
    }

    public final void registerUnits(final Collection<UnitConfig> unitConfigs) throws CouldNotPerformException, InterruptedException {
        for (UnitConfigType.UnitConfig unitConfig : unitConfigs) {
            registerUnit(unitConfig);
        }
    }

    public final void registerUnitsById(final Collection<String> unitIds) throws CouldNotPerformException, InterruptedException {
        CachedUnitRegistryRemote.waitForData();
        for (String unitId : unitIds) {
            registerUnit(unitRegistry.getUnitConfigById(unitId));
        }
    }

    //TODO mpohling: implement unit factory instead!
    public final void registerUnit(final UnitConfig unitConfig) throws CouldNotPerformException, InitializationException, InterruptedException {
        try {
            GeneratedMessage.Builder unitMessageBuilder = registerUnitBuilder(unitConfig);

            Constructor<? extends AbstractUnitController> unitConstructor;
            try {
                unitConstructor = UnitConfigToUnitClassTransformer.transform(unitConfig).getConstructor(UnitHost.class, unitMessageBuilder.getClass());
            } catch (CouldNotTransformException | NoSuchMethodException | SecurityException | NullPointerException ex) {
                throw new CouldNotPerformException("Could not instantiate Unit[" + unitConfig + "]!", ex);
            }
            AbstractUnitController unit;
            try {
                unit = unitConstructor.newInstance(this, unitMessageBuilder);
            } catch (java.lang.InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NullPointerException ex) {
                throw new CouldNotPerformException("Could not instantiate Unit[" + unitConfig + "]!", ex);
            }
            unit.init(unitConfig);
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

        } catch (Exception ex) {
            throw new CouldNotPerformException("Could register UnitBuilder[" + unitConfig + "]!", ex);
        }
    }

    private GeneratedMessage.Builder loadUnitBuilder(final UnitConfigType.UnitConfig unitConfig) throws CouldNotPerformException {
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
            throw new CouldNotPerformException("Could not load builder for " + unitConfig.getId() + "!", ex);
        }
        return builder;
    }

    public Collection<AbstractUnitController> getUnits() {
        return Collections.unmodifiableCollection(unitMap.values());
    }

    @Override
    public String toString() {
        try {
            if (hasDataField(TYPE_FIELD_ID)) {
                return getClass().getSimpleName() + "[" + getId() + "]";
            }
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not detect id!", ex), logger, LogLevel.DEBUG);
        }
        return getClass().getSimpleName() + "[?]";
    }
}
