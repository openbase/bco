/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.service;

import java.util.Collection;
import java.util.Collections;
import java.util.TreeMap;
import de.citec.dal.hal.AbstractHardwareController;

/**
 *
 * @author mpohling
 */
public class HardwareRegistry {

    private static final class InstanceHolder {

        static final HardwareRegistry INSTANCE = new HardwareRegistry();
    }

    public static HardwareRegistry getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private final TreeMap<String, AbstractHardwareController> registry;

    private HardwareRegistry() {
        this.registry = new TreeMap<>();
    }

    public void register(AbstractHardwareController hardware) {
        registry.put(hardware.getId(), hardware);
    }

    public Collection<AbstractHardwareController> getHardwareCollection() {
        return Collections.unmodifiableCollection(registry.values());
    }

    TreeMap<String, AbstractHardwareController> getHardwareMap() {
        return registry;
    }
}
