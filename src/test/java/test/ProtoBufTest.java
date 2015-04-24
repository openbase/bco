/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import java.util.HashMap;
import junit.framework.TestCase;
import org.junit.Ignore;
import org.junit.Test;
import rst.homeautomation.device.fibaro.F_FGS_221Type;
import rst.homeautomation.unit.ButtonType.Button;

/**
 *
 * @author mpohling
 */
public class ProtoBufTest extends TestCase {

    public ProtoBufTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

//    @Test
//    public void testInternalBuilderDep() {
//        F_FGS_221Type.F_FGS_221.Builder fibaro = F_FGS_221Type.F_FGS_221.newBuilder();
//        Button.Builder button = Button.newBuilder();
//        button.setName("Test1");
//        fibaro.getUnitButtonBuilderList().add(button);
//        assertEquals(fibaro.getUnitButton(0).getName(), "Test1");
//        button.setName("Test2");
//        assertEquals(fibaro.getUnitButton(0).getName(), "Test2");
//
//    }

    public void testHello() {

        HashMap map = new HashMap();
//        map.keySet().
//        DeviceConfigType.DeviceConfig.Builder newBuilder = DeviceConfigType.DeviceConfig.newBuilder();
//        
//        newBuilder.addUnitConfigs(UnitConfigType.UnitConfig.newBuilder());
//        newBuilder.addUnitConfigs(UnitConfigType.UnitConfig.newBuilder());
//        newBuilder.addUnitConfigs(UnitConfigType.UnitConfig.newBuilder());
//        List<? extends UnitConfigType.UnitConfigOrBuilder> unitConfigsBuilderList = newBuilder.getUnitConfigsOrBuilderList();
//        
//        assertEquals(newBuilder.getUnitConfigsCount(), 3);
//        assertEquals(unitConfigsBuilderList.size(), 3);
//        
//        newBuilder.getremoveUnitConfigs(1);
//        
//        assertEquals(newBuilder.getUnitConfigsCount(), 2);
//        assertEquals(unitConfigsBuilderList.size(), 2);
//        
//        unitConfigsBuilderList.remove(0);
//        
//        assertEquals(newBuilder.getUnitConfigsCount(), 1);
//        assertEquals(unitConfigsBuilderList.size(), 1);
//        
//        
//        assertEquals(newBuilder.getUnitConfigsCount(), 0);
//        assertEquals(unitConfigsBuilderList.size(), 0);
    }
}
