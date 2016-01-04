/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.citec.dal.data.transform;

import org.dc.bco.coma.dem.binding.openhab.transform.HSVColorTransformer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import rst.vision.HSVColorType;
import rst.homeautomation.openhab.HSBType.HSB;

/**
 *
 * @author nuc
 */
public class HSVColorTransformerTest {
    
    public HSVColorTransformerTest() {
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
     * Test of transform method, of class HSVColorTransformer.
     */
    @Test(timeout = 60000)
    public void testTransform_HSBType() throws Exception {
        System.out.println("transform");
        HSB color = HSB.newBuilder().setHue(345).setSaturation(30).setBrightness(50).build();
        HSVColorType.HSVColor tempResult = HSVColorTransformer.transform(color);
        HSB result = HSVColorTransformer.transform(tempResult);
        assertEquals(color, result);
    }

    /**
     * Test of transform method, of class HSVColorTransformer.
     */
    @Test(timeout = 60000)
    public void testTransform_HSVColorTypeHSVColor() throws Exception {
        System.out.println("transform");
        HSVColorType.HSVColor color = HSVColorType.HSVColor.newBuilder().setHue(111).setSaturation(56).setValue(57).build();
        HSB tempResult = HSVColorTransformer.transform(color);
        HSVColorType.HSVColor result = HSVColorTransformer.transform(tempResult);
        assertEquals(color, result);
    }
    
}
