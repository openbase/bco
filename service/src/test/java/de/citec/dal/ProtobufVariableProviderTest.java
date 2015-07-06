/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal;

import de.citec.jul.extension.protobuf.ProtobufVariableProvider;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import rst.homeautomation.unit.UnitConfigType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine
 * Threepwood</a>
 */
public class ProtobufVariableProviderTest {

    public ProtobufVariableProviderTest() {
    }

    @BeforeClass
    public static void setUpClass() {
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
     * Test of getName method, of class ProtobufVariableProvider.
     */
    @Test
    public void testGetName() {

        UnitConfigType.UnitConfig config = UnitConfig.getDefaultInstance();
        System.out.println("getName");
        ProtobufVariableProvider instance = new ProtobufVariableProvider(config);
        String expResult = "UnitConfig" + ProtobufVariableProvider.NAME_SUFIX;
        String result = instance.getName();
        assertEquals(expResult, result);
    }

    /**
     * Test of getValue method, of class ProtobufVariableProvider.
     */
    @Test
    public void testGetValue() throws Exception {
        UnitConfigType.UnitConfig config = UnitConfig.getDefaultInstance();
        config = config.toBuilder().setLabel("TestLabel").build();
        config = config.toBuilder().setId("TestID").build();
        System.out.println("getValue");
        ProtobufVariableProvider instance = new ProtobufVariableProvider(config);
        
        assertEquals("TestLabel", instance.getValue("LABEL"));
        assertEquals("TestID", instance.getValue("ID"));
    }

}
