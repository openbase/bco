package org.openbase.bco.dal.control.transform;

/*
 * #%L
 * BCO DAL Control
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
import org.openbase.bco.dal.control.layer.unit.AbstractUnitController;
import org.openbase.bco.dal.lib.layer.unit.UnitController;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.CouldNotTransformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.processing.StringProcessor;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * * @author Divine <a href="mailto:DivineThreepwood@gmail.com">Divine</a>
 */
public class UnitConfigToUnitClassTransformer {

    public static Class<? extends UnitController> transform(final UnitConfig unitConfig) throws CouldNotTransformException {

        String className = AbstractUnitController.class.getPackage().getName() + "." + StringProcessor.transformUpperCaseToCamelCase(unitConfig.getUnitType().name()) + "Controller";
        try {
            return (Class<? extends UnitController>) Class.forName(className);
        } catch (ClassNotFoundException ex) {
            try {
                throw new CouldNotTransformException(ScopeGenerator.generateStringRep(unitConfig.getScope()), UnitController.class, new NotAvailableException("Class", ex));
            } catch (CouldNotPerformException ex1) {
                throw new CouldNotTransformException(unitConfig.getLabel(), UnitController.class, new NotAvailableException("Class", ex));
            }
        }
    }
}
