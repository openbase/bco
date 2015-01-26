
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.service;

import de.citec.dal.bindings.openhab.OpenhabBinding;
import java.util.Collection;
import java.util.Collections;
import java.util.TreeMap;
import de.citec.dal.hal.AbstractDeviceController;
import de.citec.dal.hal.AbstractUnitController;
import de.citec.dal.util.NotAvailableException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mpohling
 */
public class DALRegistry {

    private static final Logger logger = LoggerFactory.getLogger(OpenhabBinding.class);
	private static DALRegistry instance;

    public synchronized static DALRegistry getInstance() {
		if(instance == null) {
			logger.debug("Registry not initialized. Create new instance...");
			instance = new DALRegistry();
		}
        return instance;
    }

    private final TreeMap<String, AbstractDeviceController> deviceRegistry;
    private final TreeMap<String, AbstractUnitController> unitRegistry;

    private final Map<Class<? extends AbstractUnitController>, List<AbstractUnitController>> registeredUnitClasses;

    private DALRegistry() {
        this.deviceRegistry = new TreeMap<>();
        this.unitRegistry = new TreeMap<>();
        this.registeredUnitClasses = new HashMap<>();
    }

    public void register(AbstractDeviceController hardware) {
        deviceRegistry.put(hardware.getId(), hardware);
        Collection<AbstractUnitController> units = hardware.getUnits();
        for (AbstractUnitController unit : units) {
            unitRegistry.put(unit.getScope().toString(), unit);
            if (!registeredUnitClasses.containsKey(unit.getClass())) {
                registeredUnitClasses.put(unit.getClass(), new ArrayList<AbstractUnitController>());
            }
            registeredUnitClasses.get(unit.getClass()).add(unit);
        }
    }

    public Collection<AbstractDeviceController> getHardwareCollection() {
        return Collections.unmodifiableCollection(deviceRegistry.values());
    }

    public TreeMap<String, AbstractDeviceController> getDeviceMap() {
        return deviceRegistry;
    }

    public TreeMap<String, AbstractUnitController> getUnitMap() {
        return unitRegistry;
    }

    public Collection<Class<? extends AbstractUnitController>> getRegisteredUnitClasses() {
        return Collections.unmodifiableSet(registeredUnitClasses.keySet());
    }
    
    public Collection<AbstractUnitController> getUnits(final Class<? extends AbstractUnitController> unitClass) throws NotAvailableException {
        if(!registeredUnitClasses.containsKey(unitClass)) {
            throw new NotAvailableException(unitClass.getSimpleName());
        }
        return Collections.unmodifiableCollection(registeredUnitClasses.get(unitClass));
    }
}
