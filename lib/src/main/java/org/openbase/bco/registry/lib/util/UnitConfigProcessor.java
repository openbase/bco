package org.openbase.bco.registry.lib.util;

/*
 * #%L
 * BCO Registry Lib
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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
import com.google.protobuf.GeneratedMessage;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.processing.StringProcessor;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitConfigType.UnitConfigOrBuilder;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
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
        verifyUnitConfig(unitConfig, unitConfig.getUnitType());
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
        if(isBaseUnit(unitConfig)) {
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
                if (!(boolean) unitConfig.getClass().getMethod("has" + StringProcessor.transformUpperCaseToCamelCase(unitType.name()) + "Config").invoke(unitConfig)) {
                    throw new VerificationFailedException("UnitType config missing of given UnitConfig!");
                }
            } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NullPointerException ex) {
                throw new VerificationFailedException("Given unit config is not compatible with current programm version!", ex);
            }
        }
    }

    public static void verifyUnit(final UnitConfig unitConfig) throws VerificationFailedException {
        verifyUnitConfig(unitConfig, unitConfig.getUnitType());
        verifyUnitType(unitConfig, unitConfig.getUnitType());
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
     * @return the unit data class name.
     */
    public static String getUnitDataClassName(final UnitType unitType) {
        return StringProcessor.transformUpperCaseToCamelCase(unitType.name()) + "Data";
    }

    /**
     * This method returns the unit class resolved by the given unit type.
     *
     * @param unitType the unit type used to extract the unit class.
     * @return the unit data class.
     * @throws org.openbase.jul.exception.NotAvailableException is thrown if the data class name could not be detected.
     */
    public static Class<? extends GeneratedMessage> getUnitDataClass(final UnitType unitType) throws NotAvailableException {
        final String unitDataClassSimpleName = getUnitDataClassName(unitType);
        final String unitDataClassName = UNIT_PACKAGE.getName() + "." + ((isBaseUnit(unitType)) ? unitType.name().toLowerCase().replaceAll("_", "") : "dal") + "." + unitDataClassSimpleName + "Type$" + unitDataClassSimpleName;
     
        try {
            return (Class<? extends GeneratedMessage>) Class.forName(unitDataClassName);
        } catch (NullPointerException | ClassNotFoundException | ClassCastException ex) {
            throw new NotAvailableException("UnitDataClass", unitDataClassName, new CouldNotPerformException("Could not detect class!", ex));
        }
    }

    /**
     * This method returns the unit class resolved by the given unit config.
     *
     * @param unitConfig the unit config used to extract the unit class.
     * @return the unit data class.
     * @throws org.openbase.jul.exception.NotAvailableException is thrown if the data class could not be detected.
     */
    public static Class<? extends GeneratedMessage> getUnitDataClass(final UnitConfig unitConfig) throws NotAvailableException {
        return UnitConfigProcessor.getUnitDataClass(unitConfig.getUnitType());
    }
}
