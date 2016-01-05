package org.dc.bco.coma.dem.binding.openhab.transform;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
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
