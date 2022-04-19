package org.openbase.bco.dal.test.layer.service;

/*-
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

import java.util.Collection;

import com.google.protobuf.ProtocolMessageEnum;
import org.junit.jupiter.api.AfterAll;
import org.junit.Assert;

import static org.junit.Assert.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.test.AbstractBCOTest;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.type.domotic.service.ServiceCommunicationTypeType.ServiceCommunicationType.CommunicationType;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.BatteryStateType;
import org.openbase.type.domotic.state.ColorStateType;
import org.openbase.type.domotic.state.MotionStateType;
import org.openbase.type.domotic.state.PowerStateType.PowerState;
import org.openbase.type.domotic.state.SmokeStateType;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class ServiceTest extends AbstractBCOTest {

    public ServiceTest() {
    }

    @BeforeAll
    public static void setUpClass() throws Throwable {
        AbstractBCOTest.setUpClass();
    }

    @AfterAll
    public static void tearDownClass() throws Throwable {
        AbstractBCOTest.tearDownClass();
    }

    /**
     * Test of getServiceStateClass method, of class Service.
     */
    @Test
    public void testDetectServiceDataClass() throws Exception {
        System.out.println("detectServiceDataClass");
        try {
            Assert.assertEquals("wrong service class detected!", Services.getServiceStateClass(ServiceType.BATTERY_STATE_SERVICE), BatteryStateType.BatteryState.class);
            assertEquals("wrong service class detected!", Services.getServiceStateClass(ServiceType.COLOR_STATE_SERVICE), ColorStateType.ColorState.class);
            assertEquals("wrong service class detected!", Services.getServiceStateClass(ServiceType.SMOKE_STATE_SERVICE), SmokeStateType.SmokeState.class);
            assertEquals("wrong service class detected!", Services.getServiceStateClass(ServiceType.MOTION_STATE_SERVICE), MotionStateType.MotionState.class);
        } catch (Exception ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, System.out);
        }
    }

    /**
     * Test of getServiceStateEnumValues method, of class Service.
     */
    @Test
    public void testGetServiceStateValues() throws Exception {
        System.out.println("getServiceStateEnumValues");
        try {
            Registries.getClassRegistry().waitForData();
            Collection<? extends ProtocolMessageEnum> values = Services.getServiceStateEnumValues(ServiceType.POWER_STATE_SERVICE);
            for (PowerState.State state : PowerState.State.values()) {
                Assert.assertTrue("Detected values does not contain " + state.name(), values.contains(state));
            }
        } catch (Exception ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, System.err);
        }
    }

    /**
     * Test of getServiceStateEnumValues method, of class Service.
     */
    @Test
    public void testGenerateServiceStateBuilder() throws Exception {
        System.out.println("getServiceStateEnumValues");
        try {
            Registries.getClassRegistry().waitForData();
            Registries.getTemplateRegistry().waitForData();

            for (ServiceType serviceType : ServiceType.values()) {
                if (serviceType == ServiceType.UNKNOWN) {
                    continue;
                }
                Services.generateServiceStateBuilder(serviceType);
            }

            for (CommunicationType communicationType : CommunicationType.values()) {
                if (communicationType == CommunicationType.UNKNOWN) {
                    continue;
                }
                Services.generateServiceStateBuilder(communicationType);
            }
        } catch (Exception ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, System.err);
        }
    }
}
