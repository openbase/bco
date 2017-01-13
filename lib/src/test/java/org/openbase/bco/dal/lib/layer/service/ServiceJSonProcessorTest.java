package org.openbase.bco.dal.lib.layer.service;

/*
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbase.bco.dal.lib.layer.service.ServiceJSonProcessor.JavaTypeToProto;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import rst.domotic.state.PowerStateType;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ServiceJSonProcessorTest {

    private final ServiceJSonProcessor serviceJSonProcessor;

    public ServiceJSonProcessorTest() {
        this.serviceJSonProcessor = new ServiceJSonProcessor();
    }

    @BeforeClass
    public static void setUpClass() throws JPServiceException {
        JPService.setupJUnitTestMode();
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getServiceAttributeType method, of class ServiceJSonProcessor.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 5000)
    public void testGetServiceAttributeType() throws Exception {
        System.out.println("getServiceAttributeType");
        Object serviceAttribute = 3.141d;
        assertEquals(JavaTypeToProto.DOUBLE.getProtoType().toString(), serviceJSonProcessor.getServiceAttributeType(serviceAttribute));

        serviceAttribute = true;
        assertEquals(JavaTypeToProto.BOOLEAN.getProtoType().toString(), serviceJSonProcessor.getServiceAttributeType(serviceAttribute));

        serviceAttribute = 2.7f;
        assertEquals(JavaTypeToProto.FLOAT.getProtoType().toString(), serviceJSonProcessor.getServiceAttributeType(serviceAttribute));

        serviceAttribute = 12;
        assertEquals(JavaTypeToProto.INTEGER.getProtoType().toString(), serviceJSonProcessor.getServiceAttributeType(serviceAttribute));

        serviceAttribute = 42l;
        assertEquals(JavaTypeToProto.LONG.getProtoType().toString(), serviceJSonProcessor.getServiceAttributeType(serviceAttribute));

        serviceAttribute = "This is a test!";
        assertEquals(JavaTypeToProto.STRING.getProtoType().toString(), serviceJSonProcessor.getServiceAttributeType(serviceAttribute));

        serviceAttribute = PowerStateType.PowerState.newBuilder().setValue(PowerStateType.PowerState.State.ON).build();
        assertEquals(serviceAttribute.getClass().getName(), serviceJSonProcessor.getServiceAttributeType(serviceAttribute));

        serviceAttribute = PowerStateType.PowerState.State.OFF;
        assertEquals(serviceAttribute.getClass().getName(), serviceJSonProcessor.getServiceAttributeType(serviceAttribute));

        serviceAttribute = JavaTypeToProto.BOOLEAN;
        assertEquals(serviceAttribute.getClass().getName(), serviceJSonProcessor.getServiceAttributeType(serviceAttribute));
    }

    /**
     * Test of de-/serialize methods, of class serviceJSonProcessor.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 5000)
    public void testSerializationPipeline() throws Exception {
        System.out.println("SerializationPipeline");
        Object serviceAttribute = 3.141d;
        assertEquals(serviceAttribute, serviceJSonProcessor.deserialize(serviceJSonProcessor.serialize(serviceAttribute), serviceJSonProcessor.getServiceAttributeType(serviceAttribute)));

        serviceAttribute = true;
        assertEquals(serviceAttribute, serviceJSonProcessor.deserialize(serviceJSonProcessor.serialize(serviceAttribute), serviceJSonProcessor.getServiceAttributeType(serviceAttribute)));

        serviceAttribute = 2.7f;
        assertEquals(serviceAttribute, serviceJSonProcessor.deserialize(serviceJSonProcessor.serialize(serviceAttribute), serviceJSonProcessor.getServiceAttributeType(serviceAttribute)));

        serviceAttribute = 12;
        assertEquals(serviceAttribute, serviceJSonProcessor.deserialize(serviceJSonProcessor.serialize(serviceAttribute), serviceJSonProcessor.getServiceAttributeType(serviceAttribute)));

        serviceAttribute = 42l;
        assertEquals(serviceAttribute, serviceJSonProcessor.deserialize(serviceJSonProcessor.serialize(serviceAttribute), serviceJSonProcessor.getServiceAttributeType(serviceAttribute)));

        serviceAttribute = "This is a test!";
        assertEquals(serviceAttribute, serviceJSonProcessor.deserialize(serviceJSonProcessor.serialize(serviceAttribute), serviceJSonProcessor.getServiceAttributeType(serviceAttribute)));

        serviceAttribute = PowerStateType.PowerState.newBuilder().setValue(PowerStateType.PowerState.State.ON).build();
        assertEquals(serviceAttribute, serviceJSonProcessor.deserialize(serviceJSonProcessor.serialize(serviceAttribute), serviceJSonProcessor.getServiceAttributeType(serviceAttribute)));

        serviceAttribute = PowerStateType.PowerState.State.OFF;
        assertEquals(serviceAttribute, serviceJSonProcessor.deserialize(serviceJSonProcessor.serialize(serviceAttribute), serviceJSonProcessor.getServiceAttributeType(serviceAttribute)));

        serviceAttribute = JavaTypeToProto.BOOLEAN;
        assertEquals(serviceAttribute, serviceJSonProcessor.deserialize(serviceJSonProcessor.serialize(serviceAttribute), serviceJSonProcessor.getServiceAttributeType(serviceAttribute)));
    }
}
