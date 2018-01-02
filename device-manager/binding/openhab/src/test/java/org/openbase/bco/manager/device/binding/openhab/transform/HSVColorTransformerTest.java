package org.openbase.bco.manager.device.binding.openhab.transform;

/*
 * #%L
 * BCO Manager Device Binding OpenHAB
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.extension.openhab.binding.transform.HSBColorTransformer;
import rst.domotic.binding.openhab.HSBType.HSB;
import rst.vision.HSBColorType.HSBColor;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 *
 */
public class HSVColorTransformerTest {

    public HSVColorTransformerTest() {
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
     * Test of transform method, of class HSVColorTransformer.
     */
    @Test(timeout = 10000)
    public void testTransform_HSBType() throws Exception {
        System.out.println("testTransform_HSBType");
        HSB color = HSB.newBuilder().setHue(345).setSaturation(30).setBrightness(50).build();
        HSBColor tempResult = HSBColorTransformer.transform(color);
        HSB result = HSBColorTransformer.transform(tempResult);
        assertEquals(color, result);
    }

    /**
     * Test of transform method, of class HSVColorTransformer.
     */
    @Test(timeout = 10000)
    public void testTransform_HSVColorTypeHSVColor() throws Exception {
        System.out.println("testTransform_HSVColorTypeHSVColor");
        HSBColor color = HSBColor.newBuilder().setHue(111).setSaturation(56).setBrightness(57).build();
        HSB tempResult = HSBColorTransformer.transform(color);
        HSBColor result = HSBColorTransformer.transform(tempResult);
        assertEquals(color, result);
    }

}
