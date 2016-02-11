package org.dc.bco.manager.device.binding.openhab.transform;

/*
 * #%L
 * COMA DeviceManager Binding OpenHAB
 * %%
 * Copyright (C) 2015 - 2016 DivineCooperation
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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import rst.homeautomation.openhab.HSBType.HSB;
import rst.vision.HSVColorType;

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
