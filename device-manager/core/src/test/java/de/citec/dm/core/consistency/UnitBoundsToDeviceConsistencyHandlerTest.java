/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dm.core.consistency;

import de.citec.jul.extension.protobuf.IdentifiableMessage;
import de.citec.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import de.citec.jul.storage.registry.ProtoBufRegistryInterface;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.unit.UnitConfigType;
import rst.homeautomation.unit.UnitTemplateType;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 */
public class UnitBoundsToDeviceConsistencyHandlerTest {
    
    public UnitBoundsToDeviceConsistencyHandlerTest() {
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
     * Test of checkDuplicatedUnitType method, of class UnitBoundsToDeviceConsistencyHandler.
     */
    @Test
    public void testCheckDuplicatedUnitType() {
        System.out.println("checkDuplicatedUnitType");
        DeviceConfigType.DeviceConfig.Builder deviceConfig = DeviceConfigType.DeviceConfig.newBuilder();
        boolean expResult;
        
        expResult = false;
        deviceConfig.addUnitConfig(UnitConfigType.UnitConfig.newBuilder().setType(UnitTemplateType.UnitTemplate.UnitType.LIGHT));
        assertEquals(expResult, UnitBoundsToDeviceConsistencyHandler.checkDuplicatedUnitType(deviceConfig.build()));
        
        expResult = true;
        deviceConfig.addUnitConfig(UnitConfigType.UnitConfig.newBuilder().setType(UnitTemplateType.UnitTemplate.UnitType.LIGHT));
        assertEquals(expResult, UnitBoundsToDeviceConsistencyHandler.checkDuplicatedUnitType(deviceConfig.build()));
    }
}
