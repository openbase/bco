package org.openbase.bco.dal.lib.layer.service;

/*
 * #%L
 * BCO DAL Library
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
import com.google.protobuf.Message;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.type.domotic.state.PowerStateType;

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


    /**
     * Test of getServiceStateClassName method, of class ServiceJSonProcessor.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 5000)
    public void testGetServiceStateClassName() throws Exception {
        System.out.println("getServiceStateClassName");
        Message serviceState;
        // outdated because service atributes must be messages.
        // TODO: move test to jul to test more generic json serialization. 
//        Object serviceState = 3.141d;
//        assertEquals(JavaTypeToProto.DOUBLE.getProtoType().toString(), serviceJSonProcessor.getServiceStateClassName(serviceState));
//
//        serviceState = true;
//        assertEquals(JavaTypeToProto.BOOLEAN.getProtoType().toString(), serviceJSonProcessor.getServiceStateClassName(serviceState));
//
//        serviceState = 2.7f;
//        assertEquals(JavaTypeToProto.FLOAT.getProtoType().toString(), serviceJSonProcessor.getServiceStateClassName(serviceState));
//
//        serviceState = 12;
//        assertEquals(JavaTypeToProto.INTEGER.getProtoType().toString(), serviceJSonProcessor.getServiceStateClassName(serviceState));
//
//        serviceState = 42l;
//        assertEquals(JavaTypeToProto.LONG.getProtoType().toString(), serviceJSonProcessor.getServiceStateClassName(serviceState));
//
//        serviceState = "This is a test!";
//        assertEquals(JavaTypeToProto.STRING.getProtoType().toString(), serviceJSonProcessor.getServiceStateClassName(serviceState));
//
//        serviceState = JavaTypeToProto.BOOLEAN;
//        assertEquals(serviceState.getClass().getName(), serviceJSonProcessor.getServiceStateClassName(serviceState));

        serviceState = PowerStateType.PowerState.newBuilder().setValue(PowerStateType.PowerState.State.ON).build();
        assertEquals(serviceState.getClass().getName(), Services.getServiceStateClassName(serviceState));

    }

    /**
     * Test of de-/serialize methods, of class serviceJSonProcessor.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 5000)
    public void testSerializationPipeline() throws Exception {
        System.out.println("SerializationPipeline");
        Message serviceState;
        // outdated because service atributes must be messages.
        // TODO: move test to jul to test more generic json serialization. 
        
//        Object serviceState = 3.141d;
//        assertEquals(serviceState, serviceJSonProcessor.deserialize(serviceJSonProcessor.serialize(serviceState), serviceJSonProcessor.getServiceStateClassName(serviceState)));
//
//        serviceState = true;
//        assertEquals(serviceState, serviceJSonProcessor.deserialize(serviceJSonProcessor.serialize(serviceState), serviceJSonProcessor.getServiceStateClassName(serviceState)));
//
//        serviceState = 2.7f;
//        assertEquals(serviceState, serviceJSonProcessor.deserialize(serviceJSonProcessor.serialize(serviceState), serviceJSonProcessor.getServiceStateClassName(serviceState)));
//
//        serviceState = 12;
//        assertEquals(serviceState, serviceJSonProcessor.deserialize(serviceJSonProcessor.serialize(serviceState), serviceJSonProcessor.getServiceStateClassName(serviceState)));
//
//        serviceState = 42l;
//        assertEquals(serviceState, serviceJSonProcessor.deserialize(serviceJSonProcessor.serialize(serviceState), serviceJSonProcessor.getServiceStateClassName(serviceState)));
//
//        serviceState = "This is a test!";
//        assertEquals(serviceState, serviceJSonProcessor.deserialize(serviceJSonProcessor.serialize(serviceState), serviceJSonProcessor.getServiceStateClassName(serviceState)));
//        
//        serviceState = JavaTypeToProto.BOOLEAN;
//        assertEquals(serviceState, serviceJSonProcessor.deserialize(serviceJSonProcessor.serialize(serviceState), serviceJSonProcessor.getServiceStateClassName(serviceState)));

        serviceState = PowerStateType.PowerState.newBuilder().setValue(PowerStateType.PowerState.State.ON).build();
        assertEquals(serviceState, serviceJSonProcessor.deserialize(serviceJSonProcessor.serialize(serviceState), Services.getServiceStateClassName(serviceState)));

    }
}
