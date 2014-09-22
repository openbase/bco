/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.citec.dal.data.transform;

import de.citec.dal.data.transform.HSVColorTransformer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.PercentType;
import rst.vision.HSVColorType;

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
    @Test
    public void testTransform_HSBType() throws Exception {
        System.out.println("transform");
        HSBType color = new HSBType(new DecimalType(345), new PercentType(30), new PercentType(50));
        HSVColorType.HSVColor tempResult = HSVColorTransformer.transform(color);
        HSBType result = HSVColorTransformer.transform(tempResult);
        assertEquals(color, result);
    }

    /**
     * Test of transform method, of class HSVColorTransformer.
     */
    @Test
    public void testTransform_HSVColorTypeHSVColor() throws Exception {
        System.out.println("transform");
        HSVColorType.HSVColor color = HSVColorType.HSVColor.newBuilder().setHue(111).setSaturation(56).setValue(57).build();
        HSBType tempResult = HSVColorTransformer.transform(color);
        HSVColorType.HSVColor result = HSVColorTransformer.transform(tempResult);
        assertEquals(color, result);
    }
    
}
