package org.dc.bco.dal.lib.layer.service;

/*
 * #%L
 * DAL Library
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
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
import rst.homeautomation.state.PowerStateType;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class ServiceJSonProcessorTest {

    private static ServiceJSonProcessor instance;

    public ServiceJSonProcessorTest() {
    }

    @BeforeClass
    public static void setUpClass() {
        instance = new ServiceJSonProcessor();
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
    @Test
    public void testGetServiceAttributeType() throws Exception {
        System.out.println("getServiceAttributeType");
        Object serviceAttribute = 3.141d;
        assertEquals(ServiceJSonProcessor.JavaTypeToProto.DOUBLE.getProtoType().toString(), instance.getServiceAttributeType(serviceAttribute));

        serviceAttribute = true;
        assertEquals(ServiceJSonProcessor.JavaTypeToProto.BOOLEAN.getProtoType().toString(), instance.getServiceAttributeType(serviceAttribute));

        serviceAttribute = 2.7f;
        assertEquals(ServiceJSonProcessor.JavaTypeToProto.FLOAT.getProtoType().toString(), instance.getServiceAttributeType(serviceAttribute));

        serviceAttribute = 12;
        assertEquals(ServiceJSonProcessor.JavaTypeToProto.INTEGER.getProtoType().toString(), instance.getServiceAttributeType(serviceAttribute));

        serviceAttribute = 42l;
        assertEquals(ServiceJSonProcessor.JavaTypeToProto.LONG.getProtoType().toString(), instance.getServiceAttributeType(serviceAttribute));

        serviceAttribute = "This is a test!";
        assertEquals(ServiceJSonProcessor.JavaTypeToProto.STRING.getProtoType().toString(), instance.getServiceAttributeType(serviceAttribute));

        serviceAttribute = PowerStateType.PowerState.newBuilder().setValue(PowerStateType.PowerState.State.ON).build();
        assertEquals(serviceAttribute.getClass().getName(), instance.getServiceAttributeType(serviceAttribute));

        serviceAttribute = PowerStateType.PowerState.State.OFF;
        assertEquals(serviceAttribute.getClass().getName(), instance.getServiceAttributeType(serviceAttribute));

        serviceAttribute = ServiceJSonProcessor.JavaTypeToProto.BOOLEAN;
        assertEquals(serviceAttribute.getClass().getName(), instance.getServiceAttributeType(serviceAttribute));
    }

    /**
     * Test of de-/serialize methods, of class ServiceJSonProcessor.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testSerializationPipeline() throws Exception {
        System.out.println("SerializationPipeline");
        Object serviceAttribute = 3.141d;
        assertEquals(serviceAttribute, instance.deserialize(instance.serialize(serviceAttribute), instance.getServiceAttributeType(serviceAttribute)));

        serviceAttribute = true;
        assertEquals(serviceAttribute, instance.deserialize(instance.serialize(serviceAttribute), instance.getServiceAttributeType(serviceAttribute)));

        serviceAttribute = 2.7f;
        assertEquals(serviceAttribute, instance.deserialize(instance.serialize(serviceAttribute), instance.getServiceAttributeType(serviceAttribute)));

        serviceAttribute = 12;
        assertEquals(serviceAttribute, instance.deserialize(instance.serialize(serviceAttribute), instance.getServiceAttributeType(serviceAttribute)));

        serviceAttribute = 42l;
        assertEquals(serviceAttribute, instance.deserialize(instance.serialize(serviceAttribute), instance.getServiceAttributeType(serviceAttribute)));

        serviceAttribute = "This is a test!";
        assertEquals(serviceAttribute, instance.deserialize(instance.serialize(serviceAttribute), instance.getServiceAttributeType(serviceAttribute)));

        serviceAttribute = PowerStateType.PowerState.newBuilder().setValue(PowerStateType.PowerState.State.ON).build();
        assertEquals(serviceAttribute, instance.deserialize(instance.serialize(serviceAttribute), instance.getServiceAttributeType(serviceAttribute)));

        serviceAttribute = PowerStateType.PowerState.State.OFF;
        assertEquals(serviceAttribute, instance.deserialize(instance.serialize(serviceAttribute), instance.getServiceAttributeType(serviceAttribute)));

        serviceAttribute = ServiceJSonProcessor.JavaTypeToProto.BOOLEAN;
        assertEquals(serviceAttribute, instance.deserialize(instance.serialize(serviceAttribute), instance.getServiceAttributeType(serviceAttribute)));
    }
}
