package org.openbase.bco.registry.unit.test;

/*-
 * #%L
 * BCO Registry Unit Test
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openbase.bco.authentication.mock.MqttIntegrationTest;
import org.openbase.bco.registry.mock.MockRegistryHolder;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openbase.type.domotic.binding.BindingConfigType.BindingConfig;
import org.openbase.type.domotic.service.ServiceDescriptionType.ServiceDescription;
import org.openbase.type.domotic.service.ServiceTemplateConfigType.ServiceTemplateConfig;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.InventoryStateType;
import org.openbase.type.domotic.state.InventoryStateType.InventoryState;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateConfigType.UnitTemplateConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.device.DeviceClassType.DeviceClass;
import org.openbase.type.domotic.unit.device.DeviceConfigType.DeviceConfig;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * @author <a href="mailto:pLeminoq@openbase.org">Tamino Huxohl</a>
 */
public abstract class AbstractBCORegistryTest extends MqttIntegrationTest {

    final Logger logger = LoggerFactory.getLogger(getClass());

    @BeforeEach
    public void setUp() throws Exception {
        try {
            MockRegistryHolder.newMockRegistry();
        } catch (Exception ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
        }
    }

    @AfterEach
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

    private UnitTemplateConfig getUnitTemplateConfig(UnitType unitType) throws CouldNotPerformException {
        Set<ServiceType> serviceTypeSet = new HashSet<>();
        UnitTemplateConfig.Builder unitTemplateConfig = UnitTemplateConfig.newBuilder().setUnitType(unitType);
        for (ServiceDescription serviceDescription : Registries.getTemplateRegistry().getUnitTemplateByType(unitType).getServiceDescriptionList()) {
            if (!serviceTypeSet.contains(serviceDescription.getServiceType())) {
                unitTemplateConfig.addServiceTemplateConfig(ServiceTemplateConfig.newBuilder().setServiceType(serviceDescription.getServiceType()));
                serviceTypeSet.add(serviceDescription.getServiceType());
            }
        }
        return unitTemplateConfig.build();
    }
}
