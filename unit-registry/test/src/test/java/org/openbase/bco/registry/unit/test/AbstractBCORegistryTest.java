package org.openbase.bco.registry.unit.test;

/*-
 * #%L
 * BCO Registry Unit Test
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.openbase.bco.registry.clazz.core.ClassRegistryController;
import org.openbase.bco.registry.clazz.remote.CachedClassRegistryRemote;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.mock.MockRegistryHolder;
import org.openbase.bco.registry.template.core.TemplateRegistryController;
import org.openbase.bco.registry.unit.core.UnitRegistryController;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rst.processing.LabelProcessor;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.SyncObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.binding.BindingConfigType.BindingConfig;
import rst.domotic.registry.ClassRegistryDataType.ClassRegistryData;
import rst.domotic.service.ServiceDescriptionType.ServiceDescription;
import rst.domotic.service.ServiceTemplateConfigType.ServiceTemplateConfig;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.InventoryStateType;
import rst.domotic.state.InventoryStateType.InventoryState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateConfigType.UnitTemplateConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.device.DeviceClassType.DeviceClass;
import rst.domotic.unit.device.DeviceConfigType.DeviceConfig;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * @author <a href="mailto:pLeminoq@openbase.org">Tamino Huxohl</a>
 */
public abstract class AbstractBCORegistryTest {

    final Logger logger = LoggerFactory.getLogger(getClass());

    MockRegistry mockRegistry;

    UnitRegistryController unitRegistry;
    ClassRegistryController classRegistry;
    TemplateRegistryController templateRegistry;

    @BeforeClass
    public static void setUpClass() throws Exception {
        JPService.setupJUnitTestMode();
    }

    @Before
    public void setUp() throws Exception {
        try {
            mockRegistry = MockRegistryHolder.newMockRegistry();

            unitRegistry = (UnitRegistryController) MockRegistry.getUnitRegistry();
            classRegistry = (ClassRegistryController) MockRegistry.getClassRegistry();
            templateRegistry = (TemplateRegistryController) MockRegistry.getTemplateRegistry();
        } catch (Exception ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
        }
    }

    @After
    public void tearDown() throws Exception {
        try {
            MockRegistryHolder.shutdownMockRegistry();
        } catch (Exception ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
        }
    }

    UnitConfig generateDeviceUnitConfig(String label, String serialNumber, DeviceClass clazz) {
        InventoryState inventoryState = InventoryState.newBuilder().setValue(InventoryStateType.InventoryState.State.IN_STOCK).build();
        DeviceConfig deviceConfig = DeviceConfig.newBuilder().setDeviceClassId(clazz.getId()).setSerialNumber(serialNumber).setInventoryState(inventoryState).build();
        UnitConfig.Builder unitConfig = UnitConfig.newBuilder().setUnitType(UnitType.DEVICE).setDeviceConfig(deviceConfig);
        LabelProcessor.addLabel(unitConfig.getLabelBuilder(), Locale.ENGLISH, label);
        return unitConfig.build();
    }

    DeviceClass generateDeviceClass(String label, String productNumber, String company, UnitType... unitTypes) throws CouldNotPerformException {
        DeviceClass.Builder deviceClass = DeviceClass.newBuilder().setProductNumber(productNumber).setCompany(company);
        LabelProcessor.addLabel(deviceClass.getLabelBuilder(), Locale.ENGLISH, label);
        deviceClass.setBindingConfig(BindingConfig.newBuilder().setBindingId("OPENHAB"));
        for (UnitType unitType : unitTypes) {
            deviceClass.addUnitTemplateConfig(getUnitTemplateConfig(unitType));
        }
        return deviceClass.build();
    }

    UnitTemplateConfig getUnitTemplateConfig(UnitType unitType) throws CouldNotPerformException {
        Set<ServiceType> serviceTypeSet = new HashSet<>();
        UnitTemplateConfig.Builder unitTemplateConfig = UnitTemplateConfig.newBuilder().setType(unitType);
        for (ServiceDescription serviceDescription : templateRegistry.getUnitTemplateByType(unitType).getServiceDescriptionList()) {
            if (!serviceTypeSet.contains(serviceDescription.getServiceType())) {
                unitTemplateConfig.addServiceTemplateConfig(ServiceTemplateConfig.newBuilder().setServiceType(serviceDescription.getServiceType()));
                serviceTypeSet.add(serviceDescription.getServiceType());
            }
        }
        return unitTemplateConfig.build();
    }

    DeviceClass registerDeviceClass(DeviceClass deviceClass) throws CouldNotPerformException, ExecutionException, InterruptedException {
        CachedClassRegistryRemote.getRegistry().addDataObserver(notifyChangeObserver);
        DeviceClass clazz = classRegistry.registerDeviceClass(deviceClass).get();
        waitForDeviceClass(clazz);
        CachedClassRegistryRemote.getRegistry().removeDataObserver(notifyChangeObserver);
        return clazz;
    }

    private final SyncObject LOCK = new SyncObject("WaitForDeviceClassLock");
    final Observer<ClassRegistryData> notifyChangeObserver = (Observer<ClassRegistryData>) (Observable<ClassRegistryData> source, ClassRegistryData data) -> {
        synchronized (LOCK) {
            LOCK.notifyAll();
        }
    };

    /**
     * Wait until the DeviceClassRemoteRegistry of the UnitRegistry contains a
     * DeviceClass.
     *
     * @param deviceClass the DeviceClass tested
     * @throws CouldNotPerformException if the class registry cannot be accessed
     */
    void waitForDeviceClass(final DeviceClass deviceClass) throws CouldNotPerformException {

        synchronized (LOCK) {
            try {
                while (!CachedClassRegistryRemote.getRegistry().containsDeviceClass(deviceClass)) {
                    LOCK.wait();
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
