package org.openbase.bco.manager.device.test.remote.unit;

/*
 * #%L
 * BCO Manager Device Test
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbase.bco.dal.lib.jp.JPResourceAllocation;
import org.openbase.bco.dal.lib.layer.service.operation.PowerStateOperationService;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.dal.remote.unit.unitgroup.UnitGroupRemote;
import org.openbase.bco.manager.device.test.AbstractBCODeviceManagerTest;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.slf4j.LoggerFactory;
import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.service.ServiceDescriptionType.ServiceDescription;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.BrightnessStateType.BrightnessState;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType;
import rst.domotic.unit.unitgroup.UnitGroupConfigType.UnitGroupConfig;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class UnitGroupRemoteTest extends AbstractBCODeviceManagerTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(UnitGroupRemoteTest.class);

    private static UnitGroupRemote unitGroupRemote;
    private static final List<Unit> UNIT_LIST = new ArrayList<>();

    @BeforeClass
    public static void setUpClass() throws Throwable {
        AbstractBCODeviceManagerTest.setUpClass();

        try {
            UnitConfig unitGrouptConfig = registerUnitGroup();
            unitGroupRemote = Units.getUnit(unitGrouptConfig, true, UnitGroupRemote.class);
        } catch (Exception ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    private static UnitConfig registerUnitGroup() throws Exception {
        ServiceDescription powerStateOperationService = ServiceDescription.newBuilder().setType(ServiceType.POWER_STATE_SERVICE).setPattern(ServicePattern.OPERATION).build();
        ServiceDescription powerStateProviderService = ServiceDescription.newBuilder().setType(ServiceType.POWER_STATE_SERVICE).setPattern(ServicePattern.PROVIDER).build();
        UnitGroupConfig.Builder unitGroupConfig = UnitGroupConfig.newBuilder().addServiceDescription(powerStateOperationService).addServiceDescription(powerStateProviderService);
        unitGroupConfig.setUnitType(UnitTemplateType.UnitTemplate.UnitType.UNKNOWN);
        for (Unit<?> unit : deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().getEntries()) {
            if (allServiceTemplatesImplementedByUnit(unitGroupConfig, unit)) {
                UNIT_LIST.add(unit);
                unitGroupConfig.addMemberId(unit.getConfig().getId());
            }
        }
        assert unitGroupConfig.getMemberIdList().size() > 0;
        UnitConfig.Builder unitConfig = UnitConfig.newBuilder().setUnitGroupConfig(unitGroupConfig).setLabel("testGroup").setType(UnitTemplateType.UnitTemplate.UnitType.UNIT_GROUP);
        unitConfig.setPlacementConfig(MockRegistry.getDefaultPlacement(Registries.getLocationRegistry().getRootLocationConfig()));
        return Registries.getUnitRegistry().registerUnitConfig(unitConfig.build()).get();
    }

    private static boolean allServiceTemplatesImplementedByUnit(UnitGroupConfig.Builder unitGroup, final Unit<?> unit) throws NotAvailableException {
        List<ServiceConfig> unitServiceConfigList = unit.getConfig().getServiceConfigList();
        Set<ServiceType> unitServiceTypeList = new HashSet<>();
        unitServiceConfigList.stream().forEach((serviceConfig) -> {
            unitServiceTypeList.add(serviceConfig.getServiceDescription().getType());
        });

        boolean servicePatternValid;
        for (ServiceDescription serviceDescription : unitGroup.getServiceDescriptionList()) {
            if (!unitServiceTypeList.contains(serviceDescription.getType())) {
                return false;
            }

            servicePatternValid = false;
            for (ServiceConfig serviceConfig : unit.getConfig().getServiceConfigList()) {
                if (serviceConfig.getServiceDescription().getPattern().equals(serviceDescription.getPattern())) {
                    servicePatternValid = true;
                }
            }

            if (!servicePatternValid) {
                return false;
            }
        }
        return true;
    }

    @Before
    public void setUp() throws InitializationException, InvalidStateException {

    }

    @After
    public void tearDown() throws CouldNotPerformException {

    }

    /**
     * Test of setPowerState method, of class UnitGroupRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testSetPowerState() throws Exception {
        System.out.println("setPowerState");
        unitGroupRemote.waitForData();
        PowerState state = PowerState.newBuilder().setValue(PowerState.State.ON).build();
        unitGroupRemote.setPowerState(state).get();

        for (final Unit<?> unit : UNIT_LIST) {
            assertEquals("Power state of unit [" + unit.getConfig().getId() + "] has not been set on!", state.getValue(), ((PowerStateOperationService) unit).getPowerState().getValue());
        }

        state = PowerState.newBuilder().setValue(PowerState.State.OFF).build();
        unitGroupRemote.setPowerState(state).get();
        for (final Unit<?> unit : UNIT_LIST) {
            assertEquals("Power state of unit [" + unit.getConfig().getId() + "] has not been set on!", state.getValue(), ((PowerStateOperationService) unit).getPowerState().getValue());
        }
    }

    /**
     * Test of getPowerState method, of class UnitGroupRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testGetPowerState() throws Exception {
        System.out.println("getPowerState");
        unitGroupRemote.waitForData();
        PowerState state = PowerState.newBuilder().setValue(PowerState.State.OFF).build();
        unitGroupRemote.setPowerState(state).get();
        assertEquals("Power state has not been set in time or the return value from the getter is different!", state.getValue(), unitGroupRemote.getPowerState().getValue());
    }

    /**
     * Test of setBrightness method, of class UnitGroupRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testSetBrightness() throws Exception {
        System.out.println("setBrightness");
        unitGroupRemote.waitForData();
        Double brightness = 75d;
        BrightnessState brightnessState = BrightnessState.newBuilder().setBrightness(brightness).setBrightnessDataUnit(BrightnessState.DataUnit.PERCENT).build();
        try {
            unitGroupRemote.setBrightnessState(brightnessState).get();
            fail("Brighntess service has been used even though the group config is only defined for power service");
        } catch (CouldNotPerformException ex) {
        }
    }

    /**
     * Test of setBrightness method, of class UnitGroupRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 10000)
    public void testGetData() throws Exception {
        System.out.println("setBrightness");
        unitGroupRemote.waitForData();
        Double brightness = 30d;
        BrightnessState brightnessState = BrightnessState.newBuilder().setBrightness(brightness).setBrightnessDataUnit(BrightnessState.DataUnit.PERCENT).build();
        try {
            unitGroupRemote.setBrightnessState(brightnessState).get();
            assertEquals("BrightnessState  has not been set in time or the return value from the unit data is different!", brightness, unitGroupRemote.getData().getBrightnessState().getBrightness(), 0.1d);
        } catch (CouldNotPerformException ex) {
        }
    }
}
