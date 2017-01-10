package org.openbase.bco.registry.lib.util;

/*
 * #%L
 * BCO Registry Lib
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import java.lang.reflect.InvocationTargetException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.processing.StringProcessor;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class UnitConfigProcessor {

    public static boolean isHostUnit(final UnitConfig unitConfig) throws CouldNotPerformException {
        verifyUnitConfig(unitConfig, unitConfig.getType());
        return isHostUnit(unitConfig.getType());
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
        verifyUnitConfig(unitConfig, unitConfig.getType());
        return isDalUnit(unitConfig.getType());
    }

    public static boolean isDalUnit(final UnitType unitType) {
        return !isBaseUnit(unitType);
    }

    public static boolean isBaseUnit(final UnitConfig unitConfig) throws CouldNotPerformException {
        verifyUnitConfig(unitConfig, unitConfig.getType());
        return isBaseUnit(unitConfig.getType());
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

    public static void verifyUnitType(final UnitConfig unitConfig, final UnitType unitType) throws VerificationFailedException {
        // verify if unit type is defined
        if (!unitConfig.hasType()) {
            throw new VerificationFailedException("UnitType not available!");
        }

        // verify unit type
        if (unitConfig.getType() != unitType) {
            throw new VerificationFailedException("UnitType verification failed. Expected[" + unitType + "] but was[" + unitConfig.getType() + "]!");
        }
    }

    public static void verifyUnitConfig(final UnitConfig unitConfig, final UnitType unitType) throws VerificationFailedException {

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
        verifyUnitConfig(unitConfig, unitConfig.getType());
        verifyUnitType(unitConfig, unitConfig.getType());
    }
}
