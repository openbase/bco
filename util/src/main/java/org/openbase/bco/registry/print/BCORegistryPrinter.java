package org.openbase.bco.registry.print;

/*
 * #%L
 * BCO Registry Utility
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
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

import org.openbase.bco.authentication.lib.jp.JPCredentialsDirectory;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jps.preset.JPDebugMode;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.rsb.com.jp.JPRSBHost;
import org.openbase.jul.extension.rsb.com.jp.JPRSBPort;
import org.openbase.jul.extension.rsb.com.jp.JPRSBTransport;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.processing.StringProcessor.Alignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.device.DeviceClassType.DeviceClass;

import java.util.*;

/**
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class BCORegistryPrinter {

    private static final Logger LOGGER = LoggerFactory.getLogger(BCORegistryPrinter.class);

    public static final String APP_NAME = "bco-print-registry";

    private static final int AMOUNT_COLUM_SPACE = 5;
    private final int LINE_LENGHT;
    private static final String COLUM_DELIMITER = "|";
    private final String LINE_DELIMITER_SMALL;
    private final String LINE_DELIMITER_FAT;

    private final Map<String, Integer> deviceNumberByClassMap;
    private final Map<UnitType, Integer> unitNumberByTypeMap;
    private final Map<ServiceType, Integer> serviceNumberByTypeMap;

    public BCORegistryPrinter() throws InterruptedException, CouldNotPerformException {

        // pre init
        deviceNumberByClassMap = new HashMap<>();
        unitNumberByTypeMap = new HashMap<>();
        serviceNumberByTypeMap = new HashMap<>();
        Registries.waitForData();

        // calculate max unit label length
        int maxUnitLabelLength = 0;

        // prepare devices
        for (DeviceClass deviceClass : Registries.getClassRegistry().getDeviceClasses()) {
            maxUnitLabelLength = Math.max(maxUnitLabelLength, LabelProcessor.getBestMatch(deviceClass.getLabel()).length());
            deviceNumberByClassMap.put(deviceClass.getId(), 0);
        }
        for (UnitConfig deviceUnitConfig : Registries.getUnitRegistry().getUnitConfigsByUnitType(UnitType.DEVICE)) {
            deviceNumberByClassMap.put(deviceUnitConfig.getDeviceConfig().getDeviceClassId(), deviceNumberByClassMap.get(deviceUnitConfig.getDeviceConfig().getDeviceClassId()) + 1);
        }

        // prepare units
        for (UnitType unitType : UnitType.values()) {
            maxUnitLabelLength = Math.max(maxUnitLabelLength, unitType.name().length());
            int unitsPerType = Registries.getUnitRegistry().getUnitConfigsByUnitType(unitType).size();
            unitNumberByTypeMap.put(unitType, unitsPerType);
        }

        // prepare services
        for (ServiceType serviceType : ServiceType.values()) {
            maxUnitLabelLength = Math.max(maxUnitLabelLength, serviceType.name().length());
            int servicesPerType = Registries.getUnitRegistry().getServiceConfigs(serviceType).size();
            serviceNumberByTypeMap.put(serviceType, servicesPerType);
        }

        // post init
        LINE_LENGHT = (COLUM_DELIMITER.length() * 2) + AMOUNT_COLUM_SPACE + 3 + maxUnitLabelLength;
        LINE_DELIMITER_FAT = StringProcessor.fillWithSpaces("", LINE_LENGHT).replaceAll(" ", "=");
        LINE_DELIMITER_SMALL = StringProcessor.fillWithSpaces("", LINE_LENGHT).replaceAll(" ", "-");

        // print device category
        System.out.println();
        System.out.println(LINE_DELIMITER_FAT);
        printEntry("Devices", Registries.getUnitRegistry().getUnitConfigsByUnitType(UnitType.DEVICE).size());
        System.out.println(LINE_DELIMITER_FAT);

        // sort devices
        List<DeviceClass> devicesList = Registries.getClassRegistry().getDeviceClasses();
        devicesList.sort(Comparator.comparing(deviceClass -> {
            try {
                return LabelProcessor.getBestMatch(deviceClass.getLabel());
            } catch (NotAvailableException ex) {
                return deviceClass.getId();
            }
        }));

        // print devices
        for (DeviceClass deviceClass : devicesList) {
            if (deviceNumberByClassMap.get(deviceClass.getId()) == 0) {
                continue;
            }
            printEntry(LabelProcessor.getBestMatch(deviceClass.getLabel()), deviceNumberByClassMap.get(deviceClass.getId()));
        }
        System.out.println(LINE_DELIMITER_SMALL);
        System.out.println();

        // print unit category
        System.out.println(LINE_DELIMITER_FAT);
        printEntry("Units", Registries.getUnitRegistry().getUnitConfigs().size());
        System.out.println(LINE_DELIMITER_FAT);

        // sort units
        List<UnitType> unitServiceList = Arrays.asList(UnitType.values());
        Collections.sort(unitServiceList, Comparator.comparing(Enum::name));

        // print units
        for (UnitType unitType : unitServiceList) {
            if (unitType == UnitType.UNKNOWN) {
                continue;
            }
            if (unitNumberByTypeMap.get(unitType) == 0) {
                continue;
            }
            printEntry(StringProcessor.transformUpperCaseToPascalCase(unitType.name()), unitNumberByTypeMap.get(unitType));
        }
        System.out.println(LINE_DELIMITER_SMALL);
        System.out.println();

        // print service category
        System.out.println(LINE_DELIMITER_FAT);
        printEntry("Services", Registries.getUnitRegistry().getServiceConfigs().size());
        System.out.println(LINE_DELIMITER_FAT);

        // sort services
        List<ServiceType> servicesServiceList = Arrays.asList(ServiceType.values());
        Collections.sort(servicesServiceList, Comparator.comparing(Enum::name));

        // print services
        for (ServiceType serviceType : servicesServiceList) {
            if (serviceType == ServiceType.UNKNOWN) {
                continue;
            }
            if (serviceNumberByTypeMap.get(serviceType) == 0) {
                continue;
            }
            printEntry(StringProcessor.transformUpperCaseToPascalCase(serviceType.name()), serviceNumberByTypeMap.get(serviceType));
        }
        System.out.println(LINE_DELIMITER_SMALL);
        System.out.println();
    }

    private void printEntry(final String context, final int amount) {
        System.out.println(COLUM_DELIMITER
                + StringProcessor.fillWithSpaces(
                StringProcessor.fillWithSpaces(
                        Integer.toString(amount),
                        AMOUNT_COLUM_SPACE,
                        Alignment.RIGHT)
                        + " x " + context,
                LINE_LENGHT - (COLUM_DELIMITER.length() * 2),
                Alignment.LEFT)
                + COLUM_DELIMITER);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        /* Setup JPService */
        JPService.setApplicationName(APP_NAME);
        JPService.registerProperty(JPDebugMode.class);
        JPService.registerProperty(JPCredentialsDirectory.class);
        JPService.registerProperty(JPRSBHost.class);
        JPService.registerProperty(JPRSBPort.class);
        JPService.registerProperty(JPRSBTransport.class);

        try {
            JPService.parseAndExitOnError(args);
        } catch (IllegalStateException ex) {
            ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.ERROR);
            LOGGER.info(APP_NAME + " finished unexpected.");
        }

        try {
            new BCORegistryPrinter();
        } catch (InterruptedException | CouldNotPerformException ex) {
            ExceptionPrinter.printHistoryAndExit(ex, LOGGER);
        }
        System.exit(0);
    }
}
