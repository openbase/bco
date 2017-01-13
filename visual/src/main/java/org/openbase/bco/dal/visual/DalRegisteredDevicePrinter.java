package org.openbase.bco.dal.visual;

/*
 * #%L
 * BCO DAL Visualisation
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
import java.util.HashMap;
import java.util.Map;
import org.openbase.bco.registry.device.remote.DeviceRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.unit.device.DeviceClassType.DeviceClass;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class DalRegisteredDevicePrinter {

    private static final Logger logger = LoggerFactory.getLogger(DalRegisteredDevicePrinter.class);

    private final String delimiter = "=============================================";

    private final Map<String, Integer> deviceNumberByClassMap;
    private final Map<UnitType, Integer> unitNumberByTypeMap;
    private final Map<ServiceType, Integer> serviceNumberByTypeMap;

    public DalRegisteredDevicePrinter() throws InterruptedException, NotAvailableException, InstantiationException, CouldNotPerformException {
        deviceNumberByClassMap = new HashMap<>();
        unitNumberByTypeMap = new HashMap<>();
        serviceNumberByTypeMap = new HashMap<>();

        DeviceRegistryRemote deviceRemote = new DeviceRegistryRemote();
        deviceRemote.init();
        deviceRemote.activate();

        for (DeviceClass deviceClass : deviceRemote.getDeviceClasses()) {
            deviceNumberByClassMap.put(deviceClass.getId(), 0);
        }
        for (UnitConfig deviceUnitConfig : deviceRemote.getDeviceConfigs()) {
            deviceNumberByClassMap.put(deviceUnitConfig.getDeviceConfig().getDeviceClassId(), deviceNumberByClassMap.get(deviceUnitConfig.getDeviceConfig().getDeviceClassId()) + 1);
        }

        System.out.println(deviceRemote.getDeviceConfigs().size() + " Devices");
        System.out.println(delimiter);
        for (DeviceClass deviceClass : deviceRemote.getDeviceClasses()) {
            System.out.println(deviceNumberByClassMap.get(deviceClass.getId()) + "\tx " + deviceClass.getLabel());
        }
        System.out.println(delimiter);

        for (UnitType unitType : UnitType.values()) {
            int unitsPerType = deviceRemote.getUnitConfigs(unitType).size();
            unitNumberByTypeMap.put(unitType, unitsPerType);
        }
        System.out.println(deviceRemote.getUnitConfigs().size() + " Units");
        System.out.println(delimiter);
        for (UnitType unitType : UnitType.values()) {
            if (unitType == UnitType.UNKNOWN) {
                continue;
            }
            System.out.println(unitNumberByTypeMap.get(unitType) + "\tx " + unitType.name());
        }
        System.out.println(delimiter);

        for (ServiceType serviceType : ServiceType.values()) {
            int servicesPerType = deviceRemote.getServiceConfigs(serviceType).size();
            serviceNumberByTypeMap.put(serviceType, servicesPerType);
        }
        System.out.println(deviceRemote.getServiceConfigs().size() + " Services");
        System.out.println(delimiter);
        for (ServiceType serviceType : ServiceType.values()) {
            if (serviceType == ServiceType.UNKNOWN) {
                continue;
            }
            System.out.println(serviceNumberByTypeMap.get(serviceType) + "\tx " + serviceType.name());
        }
        System.out.println(delimiter);

        deviceRemote.shutdown();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            new DalRegisteredDevicePrinter();
        } catch (InterruptedException | CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(ex, logger, LogLevel.TRACE);
        }
    }

}
