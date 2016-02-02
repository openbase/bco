/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.lib.transform;

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

import org.dc.bco.dal.lib.layer.unit.AbstractUnitController;
import org.dc.jul.exception.CouldNotTransformException;
import org.dc.jul.processing.StringProcessor;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author Divine <a href="mailto:DivineThreepwood@gmail.com">Divine</a>
 */
public class UnitConfigToUnitClassTransformer {

    public static Class<? extends AbstractUnitController> transform(final UnitConfig config) throws CouldNotTransformException {

        String className = AbstractUnitController.class.getPackage().getName() + "." + StringProcessor.transformUpperCaseToCamelCase(config.getType().name()) + "Controller";
        try {
            return (Class< ? extends AbstractUnitController>) Class.forName(className);
        } catch (ClassNotFoundException ex) {
            throw new CouldNotTransformException(config, AbstractUnitController.class, ex);
        }
    }
}
