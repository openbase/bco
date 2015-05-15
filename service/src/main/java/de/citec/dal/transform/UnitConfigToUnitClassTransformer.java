/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.transform;

import de.citec.dal.hal.unit.AbstractUnitController;
import de.citec.dal.hal.unit.LightController;
import de.citec.jul.exception.CouldNotTransformException;
import de.citec.jul.processing.StringProcessor;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author Divine <DivineThreepwood@gmail.com>
 */
public class UnitConfigToUnitClassTransformer {

    public static Class<? extends AbstractUnitController> transform(final UnitConfig config) throws CouldNotTransformException {

        String className = AbstractUnitController.class.getPackage().getName() + "." + StringProcessor.transformUpperCaseToCamelCase(config.getTemplate().getType().name()) + "Controller";
        try {

            return (Class< ? extends AbstractUnitController>) Class.forName(className);
        } catch (ClassNotFoundException ex) {
            throw new CouldNotTransformException(config, AbstractUnitController.class, ex);
        }
    }
}
