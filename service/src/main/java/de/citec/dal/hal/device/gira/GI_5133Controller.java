/*

* To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.device.gira;

import de.citec.dal.data.Location;
import de.citec.dal.data.transform.ButtonStateTransformer;
import de.citec.dal.exception.DALException;
import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.hal.AbstractDeviceController;
import de.citec.dal.hal.unit.ButtonController;
import de.citec.jul.exception.VerificationFailedException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.devices.gira.GI_5133Type;
import rst.homeautomation.openhab.OnOffHolderType.OnOffHolder.OnOff;

/**
 *
 * @author mpohling
 */
public class GI_5133Controller extends AbstractDeviceController<GI_5133Type.GI_5133, GI_5133Type.GI_5133.Builder> {

    private final static String COMPONENT_BUTTON_0 = "Button_0";
    private final static String COMPONENT_BUTTON_1 = "Button_1";
    private final static String COMPONENT_BUTTON_2 = "Button_2";
    private final static String COMPONENT_BUTTON_3 = "Button_3";
    private final static String COMPONENT_BUTTON_4 = "Button_4";
    private final static String COMPONENT_BUTTON_5 = "Button_5";

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(GI_5133Type.GI_5133.getDefaultInstance()));
    }

    private final ButtonController button_0;
    private final ButtonController button_1;
    private final ButtonController button_2;
    private final ButtonController button_3;
    private final ButtonController button_4;
    private final ButtonController button_5;

    public GI_5133Controller(final String id, final String label, final String[] unitLabel, final Location location) throws DALException, VerificationFailedException {
        super(id, label, location, GI_5133Type.GI_5133.newBuilder());

        data.setId(id);
        this.button_0 = new ButtonController(COMPONENT_BUTTON_0, unitLabel[0], this, data.getButton0Builder());
        this.button_1 = new ButtonController(COMPONENT_BUTTON_1, unitLabel[1], this, data.getButton1Builder());
        this.button_2 = new ButtonController(COMPONENT_BUTTON_2, unitLabel[2], this, data.getButton2Builder());
        this.button_3 = new ButtonController(COMPONENT_BUTTON_3, unitLabel[3], this, data.getButton3Builder());
        this.button_4 = new ButtonController(COMPONENT_BUTTON_4, unitLabel[4], this, data.getButton4Builder());
        this.button_5 = new ButtonController(COMPONENT_BUTTON_5, unitLabel[5], this, data.getButton5Builder());
        this.register(button_0);
        this.register(button_1);
        this.register(button_2);
        this.register(button_3);
        this.register(button_4);
        this.register(button_5);
    }
    
    @Override
    protected void initHardwareMapping() throws NoSuchMethodException, SecurityException {
        halFunctionMapping.put(COMPONENT_BUTTON_0, getClass().getMethod("updateButton_0", OnOff.class));
        halFunctionMapping.put(COMPONENT_BUTTON_1, getClass().getMethod("updateButton_1", OnOff.class));
        halFunctionMapping.put(COMPONENT_BUTTON_2, getClass().getMethod("updateButton_2", OnOff.class));
        halFunctionMapping.put(COMPONENT_BUTTON_3, getClass().getMethod("updateButton_3", OnOff.class));
        halFunctionMapping.put(COMPONENT_BUTTON_4, getClass().getMethod("updateButton_4", OnOff.class));
        halFunctionMapping.put(COMPONENT_BUTTON_5, getClass().getMethod("updateButton_5", OnOff.class));
    }

    public void updateButton_0(OnOff type) throws RSBBindingException {
        try {
            logger.debug("Try to update button 0");
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
    public void updateButton_4(OnOff type) throws RSBBindingException {
        try {
            button_4.updateButtonState(ButtonStateTransformer.transform(type));
        } catch (RSBBindingException ex) {
            logger.error("Could not update button 4.", ex);
        }
    }
    public void updateButton_5(OnOff type) throws RSBBindingException {
        try {
            button_5.updateButtonState(ButtonStateTransformer.transform(type));
        } catch (RSBBindingException ex) {
            logger.error("Could not update button 5.", ex);
        }
    }
}