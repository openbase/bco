package org.openbase.bco.dal.test.layer.unit.unitgroup;

/*
 * #%L
 * BCO DAL Test
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.AfterEach;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.authentication.lib.future.AuthenticatedValueFuture;
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.layer.service.operation.PowerStateOperationService;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.remote.layer.unit.unitgroup.UnitGroupRemote;
import org.openbase.bco.dal.test.layer.unit.location.AbstractBCOLocationManagerTest;
import org.openbase.bco.registry.mock.MockRegistry;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import org.openbase.type.domotic.service.ServiceConfigType.ServiceConfig;
import org.openbase.type.domotic.service.ServiceDescriptionType.ServiceDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.PowerStateType.PowerState;
import org.openbase.type.domotic.state.PowerStateType.PowerState.State;
import org.openbase.type.domotic.state.TemperatureStateType.TemperatureState;
import org.openbase.type.domotic.state.TemperatureStateType.TemperatureState.DataUnit;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType;
import org.openbase.type.domotic.unit.unitgroup.UnitGroupConfigType.UnitGroupConfig;
import org.openbase.type.language.LabelType.Label;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class UnitGroupRemoteTest extends AbstractBCOLocationManagerTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(UnitGroupRemoteTest.class);

    private static UnitGroupRemote unitGroupRemote;
    private static final List<Unit> UNIT_LIST = new ArrayList<>();

    @BeforeAll
    public static void setUpClass() throws Throwable {
        AbstractBCOLocationManagerTest.setUpClass();

        try {
            UnitConfig unitGroupConfig = registerUnitGroup();
            unitGroupRemote = Units.getUnit(unitGroupConfig, true, UnitGroupRemote.class);
        } catch (Exception ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    private static UnitConfig registerUnitGroup() throws Exception {
        ServiceDescription powerStateOperationService = ServiceDescription.newBuilder().setServiceType(ServiceType.POWER_STATE_SERVICE).setPattern(ServicePattern.OPERATION).build();
        ServiceDescription powerStateProviderService = ServiceDescription.newBuilder().setServiceType(ServiceType.POWER_STATE_SERVICE).setPattern(ServicePattern.PROVIDER).build();
        UnitGroupConfig.Builder unitGroupConfig = UnitGroupConfig.newBuilder().addServiceDescription(powerStateOperationService).addServiceDescription(powerStateProviderService);
        unitGroupConfig.setUnitType(UnitTemplateType.UnitTemplate.UnitType.UNKNOWN);
        for (Unit<?> unit : deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().getEntries()) {
            if (allServiceTemplatesImplementedByUnit(unitGroupConfig, unit)) {
                UNIT_LIST.add(unit);
                unitGroupConfig.addMemberId(unit.getConfig().getId());
            }
        }
        assert unitGroupConfig.getMemberIdList().size() > 0;
        UnitConfig.Builder unitConfig = UnitConfig.newBuilder().setUnitGroupConfig(unitGroupConfig).setLabel(LabelProcessor.addLabel(Label.newBuilder(), Locale.ENGLISH, "testGroup")).setUnitType(UnitTemplateType.UnitTemplate.UnitType.UNIT_GROUP);
        unitConfig.setPlacementConfig(MockRegistry.getDefaultPlacement(Registries.getUnitRegistry().getRootLocationConfig()));
        return Registries.getUnitRegistry().registerUnitConfig(unitConfig.build()).get();
    }

    private static boolean allServiceTemplatesImplementedByUnit(UnitGroupConfig.Builder unitGroup, final Unit<?> unit) throws NotAvailableException {
        List<ServiceConfig> unitServiceConfigList = unit.getConfig().getServiceConfigList();
        Set<ServiceType> unitServiceTypeList = new HashSet<>();
        unitServiceConfigList.stream().forEach((serviceConfig) -> {
            unitServiceTypeList.add(serviceConfig.getServiceDescription().getServiceType());
        });

        boolean servicePatternValid;
        for (ServiceDescription serviceDescription : unitGroup.getServiceDescriptionList()) {
            if (!unitServiceTypeList.contains(serviceDescription.getServiceType())) {
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

    @BeforeEach
    public void setUp() throws InitializationException, InvalidStateException {

    }

    @AfterEach
    public void tearDown() throws CouldNotPerformException {

    }

    /**
     * Test of setPowerState method, of class UnitGroupRemote.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Timeout(10)
    public void testSetPowerState() throws Exception {
        System.out.println("setPowerState");
        unitGroupRemote.waitForData();
        PowerState state = PowerState.newBuilder().setValue(PowerState.State.ON).build();
        waitForExecution(unitGroupRemote.setPowerState(state));

        for (final Unit<?> unit : UNIT_LIST) {
            assertEquals("Power state of unit [" + unit.getConfig().getId() + "] has not been set on!", state.getValue(), ((PowerStateOperationService) unit).getPowerState().getValue());
        }

        state = PowerState.newBuilder().setValue(PowerState.State.OFF).build();
        waitForExecution(unitGroupRemote.setPowerState(state));
        for (final Unit<?> unit : UNIT_LIST) {
            assertEquals("Power state of unit [" + unit.getConfig().getId() + "] has not been set on!", state.getValue(), ((PowerStateOperationService) unit).getPowerState().getValue());
        }
    }

    /**
     * Test of getPowerState method, of class UnitGroupRemote.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Timeout(10)
    public void testGetPowerState() throws Exception {
        System.out.println("getPowerState");
        unitGroupRemote.waitForData();
        PowerState state = PowerState.newBuilder().setValue(PowerState.State.OFF).build();
        waitForExecution(unitGroupRemote.setPowerState(state));
        assertEquals("Power state has not been set in time or the return value from the getter is different!", state.getValue(), unitGroupRemote.getPowerState().getValue());
    }

    /**
     * Test if controlling an unsupported service of a unit group fails.
     *
     * @throws java.lang.Exception if something fails.
     */
    @Test
    @Timeout(10)
    public void testSetUnsupportedService() throws Exception {
        System.out.println("testSetUnsupportedService");

        unitGroupRemote.waitForData();
        TemperatureState temperatureState = TemperatureState.newBuilder().setTemperature(20).setTemperatureDataUnit(DataUnit.CELSIUS).build();

        ExceptionPrinter.setBeQuit(true);
        try {
            unitGroupRemote.setTargetTemperatureState(temperatureState).get();
            fail("UnitGroup allows to set the unsupported target temperature state!");
        } catch (ExecutionException ex) {
            // do nothing because this action should fail
        } finally {
            ExceptionPrinter.setBeQuit(false);
        }
    }

    /**
     * Test the applyActionAuthenticated method of the unit group remote.
     *
     * @throws Exception if something fails.
     */
    @Test
    @Timeout(5)
    public void testApplyActionAuthenticated() throws Exception {
        System.out.println("testApplyActionAuthenticated");

        // wait for data
        unitGroupRemote.waitForData();
        // init power to off
        waitForExecution(unitGroupRemote.setPowerState(State.OFF));

        // init authenticated value
        final PowerState serviceState = PowerState.newBuilder().setValue(State.ON).build();
        final ActionDescription actionDescription = ActionDescriptionProcessor.generateActionDescriptionBuilder(serviceState, ServiceType.POWER_STATE_SERVICE, unitGroupRemote).build();
        final AuthenticatedValue authenticatedValue = SessionManager.getInstance().initializeRequest(actionDescription, null);

        // perform request
        final AuthenticatedValueFuture<ActionDescription> future = new AuthenticatedValueFuture<>(unitGroupRemote.applyActionAuthenticated(authenticatedValue), ActionDescription.class, authenticatedValue.getTicketAuthenticatorWrapper(), SessionManager.getInstance());
        // wait for request
        waitForExecution(future);

        while (unitGroupRemote.getPowerState().getValue() != serviceState.getValue()) {
            // sleep until state is published back to location
            Thread.sleep(10);
        }

        // test if new value has been set
        assertEquals(serviceState.getValue(), unitGroupRemote.getPowerState().getValue());
    }
}
