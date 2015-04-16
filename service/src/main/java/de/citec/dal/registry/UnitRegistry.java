/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.registry;

import de.citec.dal.data.Location;
import de.citec.dal.hal.unit.AbstractUnitController;
import de.citec.dal.hal.unit.Unit;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.storage.registry.Registry;
import java.util.HashMap;

/**
 *
 * @author Divine Threepwood
 */
public class UnitRegistry extends Registry<String, Unit> {

    public UnitRegistry() throws InstantiationException {
    }

    public UnitRegistry(HashMap<String, Unit> entryMap) throws InstantiationException {
        super(entryMap);
    }

    public Unit getUnit(String label, Location location, Class<? extends AbstractUnitController> unitClass) throws CouldNotPerformException {
        return get(AbstractUnitController.generateID(label, location, unitClass));
    }
}
