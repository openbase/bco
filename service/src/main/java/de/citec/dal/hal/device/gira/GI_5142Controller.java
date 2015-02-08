/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.device.gira;

import de.citec.dal.bindings.openhab.AbstractOpenHABDeviceController;
import de.citec.dal.data.Location;
import de.citec.dal.bindings.openhab.transform.ButtonStateTransformer;
import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.hal.unit.ButtonController;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.VerificationFailedException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.devices.gira.GI_5142Type;
import rst.homeautomation.openhab.OnOffHolderType.OnOffHolder.OnOff;

/**
 *
 * @author mpohling
 */
public class GI_5142Controller extends AbstractOpenHABDeviceController<GI_5142Type.GI_5142, GI_5142Type.GI_5142.Builder> {

//    private final static String COMPONENT_BUTTON_0 = "Button_0";
//    private final static String COMPONENT_BUTTON_1 = "Button_1";
//    private final static String COMPONENT_BUTTON_2 = "Button_2";
//    private final static String COMPONENT_BUTTON_3 = "Button_3";

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(GI_5142Type.GI_5142.getDefaultInstance()));
    }

    private final ButtonController button_0;
    private final ButtonController button_1;
    private final ButtonController button_2;
    private final ButtonController button_3;

    public GI_5142Controller(final String id, final String label, final String[] unitLabel, final Location location) throws VerificationFailedException, InstantiationException {
        super(id, label, location, GI_5142Type.GI_5142.newBuilder());
        data.setId(id);
        this.button_0 = new ButtonController(unitLabel[0], this, data.getButton0Builder());
        this.button_1 = new ButtonController(unitLabel[1], this, data.getButton1Builder());
        this.button_2 = new ButtonController(unitLabel[2], this, data.getButton2Builder());
        this.button_3 = new ButtonController(unitLabel[3], this, data.getButton3Builder());
        this.registerUnit(button_0);
        this.registerUnit(button_1);
        this.registerUnit(button_2);
        this.registerUnit(button_3);
    }


//	//TODO mpohling: Resolve mapping by service not by unit type.
//    @Override
//    protected void initHardwareMapping() throws NoSuchMethodException, SecurityException {
//        halFunctionMapping.put(COMPONENT_BUTTON_0, getClass().getMethod("updateButton_0", OnOff.class));
//        halFunctionMapping.put(COMPONENT_BUTTON_1, getClass().getMethod("updateButton_1", OnOff.class));
//        halFunctionMapping.put(COMPONENT_BUTTON_2, getClass().getMethod("updateButton_2", OnOff.class));
//        halFunctionMapping.put(COMPONENT_BUTTON_3, getClass().getMethod("updateButton_3", OnOff.class));
//    }

    public void updateButton_0(OnOff type) throws RSBBindingException {
        try {
            button_0.updateButtonState(ButtonStateTransformer.transform(type));
        } catch (RSBBindingException ex) {
            logger.error("Could not update button 0.", ex);
        }
    }

    public void updateButton_1(OnOff type) throws RSBBindingException {
        try {
            button_1.updateButtonState(ButtonStateTransformer.transform(type));
        } catch (RSBBindingException ex) {
            logger.error("Could not update button 1.", ex);
        }
    }

    public void updateButton_2(OnOff type) throws RSBBindingException {
        try {
            button_2.updateButtonState(ButtonStateTransformer.transform(type));
        } catch (RSBBindingException ex) {
            logger.error("Could not update button 2.", ex);
        }
    }

    public void updateButton_3(OnOff type) throws RSBBindingException {
        try {
            button_3.updateButtonState(ButtonStateTransformer.transform(type));
        } catch (RSBBindingException ex) {
            logger.error("Could not update button 3.", ex);
        }
    }
}
