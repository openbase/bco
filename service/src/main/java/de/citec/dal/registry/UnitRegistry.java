/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.registry;

import de.citec.dal.hal.unit.Unit;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.storage.registry.Registry;
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
}
