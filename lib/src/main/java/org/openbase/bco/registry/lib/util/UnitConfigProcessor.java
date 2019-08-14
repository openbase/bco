package org.openbase.bco.registry.lib.util;

/*
 * #%L
 * BCO Registry Lib
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
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
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.protobuf.processing.ProtoBufFieldProcessor;
import org.openbase.jul.extension.type.processing.ScopeProcessor;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.extension.type.processing.ScopeProcessor;
import org.openbase.jul.processing.StringProcessor;
import org.slf4j.LoggerFactory;
import org.openbase.type.domotic.state.EnablingStateType;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfigOrBuilder;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class UnitConfigProcessor {

    public static final Package UNIT_PACKAGE = UnitConfig.class.getPackage();

    private static final List<UnitType> DAL_UNIT_TYPE_LIST = new ArrayList<>();

    public static boolean isHostUnit(final UnitConfig unitConfig) throws CouldNotPerformException {
        verifyUnitConfig(unitConfig, unitConfig.getUnitType());
        return isHostUnit(unitConfig.getUnitType());
    }

    public static boolean isHostUnit(final UnitType unitType) {
        switch (unitType) {
            case APP:
            case DEVICE:
                return true;
            default:
                return false;
        }
    }

    public static boolean isDalUnit(final UnitConfig unitConfig) throws CouldNotPerformException {
        verifyUnitConfig(unitConfig, unitConfig.getUnitType());
        return isDalUnit(unitConfig.getUnitType());
    }

    public static boolean isDalUnit(final UnitType unitType) {
        return !isBaseUnit(unitType);
    }

    public static boolean isBaseUnit(final UnitConfigOrBuilder unitConfig) throws CouldNotPerformException {
        try {
            verifyUnitConfig(unitConfig, unitConfig.getUnitType());
        } catch (VerificationFailedException ex) {
            ExceptionPrinter.printHistory("Invalid UnitConfig[" + UnitConfigProcessor.getDefaultAlias(unitConfig, "?") + "] detected!", ex, LoggerFactory.getLogger(UnitConfigProcessor.class), LogLevel.WARN);
        }
        return isBaseUnit(unitConfig.getUnitType());
    }

    public static boolean isBaseUnit(final UnitType unitType) {
        switch (unitType) {
            case AGENT:
            case APP:
            case AUTHORIZATION_GROUP:
            case CONNECTION:
            case DEVICE:
            case LOCATION:
            case SCENE:
            case UNIT_GROUP:
            case USER:
                return true;
            default:
                return false;
        }
    }

    public static boolean isVirtualUnit(final UnitConfigOrBuilder unitConfig) throws CouldNotPerformException {

        // base units are never virtual!
        if (isBaseUnit(unitConfig)) {
            return false;
        }

        return unitConfig.getUnitHostId().equals(unitConfig.getId());
    }


    public static void verifyUnitType(final UnitConfigOrBuilder unitConfig, final UnitType unitType) throws VerificationFailedException {
        // verify if unit type is defined
        if (!unitConfig.hasUnitType()) {
            throw new VerificationFailedException("UnitType not available!");
        }

        // verify unit type
        if (unitConfig.getUnitType() != unitType) {
            throw new VerificationFailedException("UnitType verification failed. Expected[" + unitType + "] but was[" + unitConfig.getUnitType() + "]!");
        }
    }

    public static void verifyUnitConfig(final UnitConfigOrBuilder unitConfig, final UnitType unitType) throws VerificationFailedException {

        verifyUnitType(unitConfig, unitType);

        // verify unit type config
        if (isBaseUnit(unitType)) {
            try {
                final FieldDescriptor unitTypeFieldDescriptor = getUnitTypeFieldDescriptor(unitConfig);
                if (!unitConfig.hasField(unitTypeFieldDescriptor)) {
                    throw new VerificationFailedException("Custom unit config field [" + unitTypeFieldDescriptor.getName() + "] is not set for UnitConfig[" + LabelProcessor.getBestMatch(unitConfig.getLabel(), UnitConfigProcessor.getDefaultAlias(unitConfig, "?")) + "]!");
                }
            } catch (NotAvailableException | NullPointerException | IndexOutOfBoundsException ex) {
                throw new VerificationFailedException("Given unit config is invalid or not compatible with current program version!", ex);
            }
        }
    }

    public static void verifyUnitConfig(final UnitConfig unitConfig) throws VerificationFailedException {
        verifyUnitConfig(unitConfig, unitConfig.getUnitType());
        verifyUnitType(unitConfig, unitConfig.getUnitType());
    }

    /**
     * Verify if the unit is enabled.
     *
     * @param unitConfig the config to identify the unit.
     *
     * @throws VerificationFailedException is thrown if the unit is disabled.
     */
    public static void verifyEnablingState(final UnitConfig unitConfig) throws VerificationFailedException {
        if (!isEnabled(unitConfig)) {
            throw new VerificationFailedException("Referred Unit[" + UnitConfigProcessor.getDefaultAlias(unitConfig, "?") + "] is disabled!");
        }
    }

    /**
     * Method returns if the unit referred by the given config is currently enabled.
     *
     * @param unitConfig the config to identify the unit.
     *
     * @return true if enabled otherwise false.
     */
    public static boolean isEnabled(final UnitConfig unitConfig) {
        return unitConfig.getEnablingState().getValue().equals(EnablingStateType.EnablingState.State.ENABLED);
    }

    public synchronized static List<UnitType> getDalUnitTypes() {
        if (DAL_UNIT_TYPE_LIST.isEmpty()) {
            for (final UnitType unitType : UnitType.values()) {
                if (isDalUnit(unitType)) {
                    DAL_UNIT_TYPE_LIST.add(unitType);
                }
            }
        }
        return DAL_UNIT_TYPE_LIST;
    }

    /**
     * This method returns the unit class name resolved by the given unit type.
     *
     * @param unitType the unit type to extract the class name.
     *
     * @return the unit data class name.
     */
    public static String getUnitDataClassName(final UnitType unitType) {
        return StringProcessor.transformUpperCaseToPascalCase(unitType.name()) + "Data";
    }

    /**
     * This method returns the unit class resolved by the given unit type.
     *
     * @param unitType the unit type used to extract the unit class.
     *
     * @return the unit data class.
     *
     * @throws org.openbase.jul.exception.NotAvailableException is thrown if the data class name could not be detected.
     */
    public static Class<? extends Message> getUnitDataClass(final UnitType unitType) throws NotAvailableException {
        final String unitDataClassSimpleName = getUnitDataClassName(unitType);
        final String unitDataClassName = UNIT_PACKAGE.getName() + "." + ((isBaseUnit(unitType)) ? unitType.name().toLowerCase().replaceAll("_", "") : "dal") + "." + unitDataClassSimpleName + "Type$" + unitDataClassSimpleName;

        try {
            return (Class<? extends Message>) Class.forName(unitDataClassName);
        } catch (NullPointerException | ClassNotFoundException | ClassCastException ex) {
            throw new NotAvailableException("UnitDataClass", unitDataClassName, new CouldNotPerformException("Could not detect class!", ex));
        }
    }

    /**
     * This method returns the unit class resolved by the given unit config.
     *
     * @param unitConfig the unit config used to extract the unit class.
     *
     * @return the unit data class.
     *
     * @throws org.openbase.jul.exception.NotAvailableException is thrown if the data class could not be detected.
     */
    public static Class<? extends Message> getUnitDataClass(final UnitConfig unitConfig) throws NotAvailableException {
        return UnitConfigProcessor.getUnitDataClass(unitConfig.getUnitType());
    }

    public static Message.Builder generateUnitDataBuilder(final UnitConfig unitConfig) throws CouldNotPerformException {
        Message.Builder builder;
        try {
            String unitTypeName = StringProcessor.transformUpperCaseToPascalCase(unitConfig.getUnitType().name());
            String unitMessageClassName = "org.openbase.type.domotic.unit.dal." + unitTypeName + "DataType$" + unitTypeName + "Data";
            Class messageClass;
            try {
                messageClass = Class.forName(unitMessageClassName);
            } catch (ClassNotFoundException ex) {
                throw new CouldNotPerformException("Could not find builder Class[" + unitMessageClassName + "]!", ex);
            }

            try {
                builder = (Message.Builder) messageClass.getMethod("newBuilder").invoke(null);
            } catch (NoSuchMethodException | SecurityException ex) {
                throw new CouldNotPerformException("Could not instantiate builder out of Class[" + messageClass.getName() + "]!", ex);
            }

        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not load builder for " + ScopeProcessor.generateStringRep(unitConfig.getScope()) + "!", ex);
        }
        return builder;
    }

    /**
     * Method resolves the field descriptor of the custom type field of the unit config.
     * For example in case a UnitConfig of UnitType.Location is passed those method would return the descriptor referring the LocationConfig field of the UnitConfig.
     *
     * @param unitConfig the config used to resolve the unit type and the field descriptor.
     *
     * @return a field descriptor which is referring the custom type field.
     *
     * @throws NotAvailableException is thrown in case the descriptor could not be resolved.
     */
    public static FieldDescriptor getUnitTypeFieldDescriptor(final UnitConfigOrBuilder unitConfig) throws NotAvailableException {
        return getUnitTypeFieldDescriptor(unitConfig.getUnitType(), unitConfig);
    }

    /**
     * Method resolves the field descriptor of the custom type field of the unit config.
     * For example in case a UnitConfig of UnitType.Location is passed those method would return the descriptor referring the LocationConfig field of the UnitConfig.
     *
     * @param unitType   refers the type of config.
     * @param unitConfig the config used to resolve the field descriptor.
     *
     * @return a field descriptor which is referring the custom type field.
     *
     * @throws NotAvailableException is thrown in case the descriptor could not be resolved.
     */
    public static FieldDescriptor getUnitTypeFieldDescriptor(final UnitType unitType, final UnitConfigOrBuilder unitConfig) throws NotAvailableException {
        final FieldDescriptor unitTypeFieldDescriptor = ProtoBufFieldProcessor.getFieldDescriptor(unitConfig, unitType.name().toLowerCase() + "_config");
        if (unitTypeFieldDescriptor == null) {
            throw new NotAvailableException("UnitConfig", "UnitTypeFieldDescriptor");
        }
        return unitTypeFieldDescriptor;
    }

    /**
     * Method returns the default alias of the given unit config.
     *
     * @param unitConfig  the alias provider.
     * @param alternative this string is returned if the default alias is not available.
     *
     * @return the default alias as string.
     */
    public static String getDefaultAlias(final UnitConfigOrBuilder unitConfig, final String alternative) {
        try {
            return getDefaultAlias(unitConfig);
        } catch (NotAvailableException e) {
            return alternative;
        }
    }

    /**
     * Method returns the default alias of the given unit config.
     *
     * @param unitConfig the alias provider.
     *
     * @return the default alias as string.
     *
     * @throws NotAvailableException is thrown if the given unit config does not provide a default alias.
     */
    public static String getDefaultAlias(final UnitConfigOrBuilder unitConfig) throws NotAvailableException {
        if (unitConfig.getAliasCount() <= 0) {
            throw new NotAvailableException("Default Alias");
        }
        return unitConfig.getAlias(0);
    }

    /**
     * Method checks if the unit referred via the unitConfig is managed by an host unit.
     * <p>
     * Since only apps and devices are host units, they do not link to any host unit.
     *
     * @param unitConfig config identifies the unit to check.
     *
     * @return true if the host unit is available, otherwise false in case the unit itself is already a host unit.
     */
    public static boolean isHostUnitAvailable(final UnitConfigOrBuilder unitConfig) {
        // in case the host unit field is referring the id of the unit itself, then no host unit is registered.
        return unitConfig.hasUnitHostId() && !unitConfig.getUnitHostId().isEmpty() && !unitConfig.getUnitHostId().equals(unitConfig.getId());
    }
}
