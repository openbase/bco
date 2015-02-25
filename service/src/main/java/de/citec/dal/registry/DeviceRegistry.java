
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.registry;

import de.citec.dal.hal.device.Device;
import de.citec.jul.exception.NotAvailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mpohling
 */
public class DeviceRegistry extends AbstractRegistry<String, Device> {

	private static final Logger logger = LoggerFactory.getLogger(DeviceRegistry.class);
	private static DeviceRegistry instance;

	public synchronized static DeviceRegistry getInstance() throws NotAvailableException {
		if (instance == null) {
			throw new NotAvailableException(DeviceRegistry.class);
		}
		return instance;
	}

//    private final TreeMap<String, AbstractDeviceController> deviceRegistry;
//    private final TreeMap<String, AbstractUnitController> unitRegistry;
//    private final Map<Class<? extends AbstractUnitController>, List<AbstractUnitController>> registeredUnitClasses;
	public DeviceRegistry() {
		instance = this;
//        this.deviceRegistry = new TreeMap<>();
//        this.unitRegistry = new TreeMap<>();
//        this.registeredUnitClasses = new HashMap<>();
	}

//    public void register(AbstractDeviceController hardware) {
//
////        if(deviceRegistry.containsKey(hardware.getId())) {
////            logger.error("Ignore registration of "+hardware+", device already registered!");
////            return;
////        } //FIXME: Unit test are not running if dublicated registration is avoided.
//
//        deviceRegistry.put(hardware.getId(), hardware);
//        Collection<AbstractUnitController> units = hardware.getUnits();
//        for (AbstractUnitController unit : units) {
//            unitRegistry.put(unit.getScope().toString(), unit);
//            if (!registeredUnitClasses.containsKey(unit.getClass())) {
//                registeredUnitClasses.put(unit.getClass(), new ArrayList<AbstractUnitController>());
//            }
//            registeredUnitClasses.get(unit.getClass()).add(unit);
//        }
//    }
//
//    public TreeMap<String, AbstractDeviceController> getDeviceMap() {
//        return deviceRegistry;
//    }
//
//    public TreeMap<String, AbstractUnitController> getUnitMap() {
//        return unitRegistry;
//    }
//    public Collection<Class<? extends AbstractUnitController>> getRegisteredUnitClasses() {
//        return Collections.unmodifiableSet(registeredUnitClasses.keySet());
//    }
//
//    public Collection<AbstractUnitController> getUnits(final Class<? extends AbstractUnitController> unitClass) throws NotAvailableException {
//        if (!registeredUnitClasses.containsKey(unitClass)) {
//            throw new NotAvailableException(unitClass.getSimpleName());
//        }
//        return Collections.unmodifiableCollection(registeredUnitClasses.get(unitClass));
//    }
//
//    public AbstractDeviceController getDevice(String unitName) throws NotAvailableException {
//        AbstractDeviceController device;
//        try {
//            Map.Entry<String, AbstractDeviceController> floorEntry = deviceRegistry.floorEntry(unitName);
//            device = floorEntry.getValue();
//        } catch (NullPointerException ex) {
//            throw new NotAvailableException("Item[" + unitName + "] not registered!", ex);
//        }
//        if (!unitName.startsWith(device.getId())) {
//            throw new NotAvailableException("Item[" + unitName + "] not registered!");
//        }
//        return device;
//    }
}
