/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.lib.layer.unit;

import org.dc.jul.exception.InstantiationException;
import org.dc.jul.storage.registry.RegistryImpl;
import java.util.HashMap;

/**
 *
 * @author Divine Threepwood
 */
public class UnitControllerRegistryImpl extends RegistryImpl<String, Unit> implements UnitControllerRegistry {

    public UnitControllerRegistryImpl() throws InstantiationException {
    }

    public UnitControllerRegistryImpl(HashMap<String, Unit> entryMap) throws InstantiationException {
        super(entryMap);
    }
}
