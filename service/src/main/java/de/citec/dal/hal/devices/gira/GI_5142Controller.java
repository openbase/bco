/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.devices.gira;

import de.citec.dal.data.Location;
import de.citec.dal.data.transform.ButtonStateTransformer;
import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.hal.AbstractHardwareController;
import de.citec.dal.hal.al.ButtonController;
import org.openhab.core.library.types.OnOffType;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.devices.gira.GI_5133Type;

/**
 *
 * @author mpohling
 */
public class GI_5142Controller extends AbstractHardwareController<GI_5133Type.GI_5133, GI_5133Type.GI_5133.Builder> {

    private final static String COMPONENT_BUTTON_0 = "Button_0";
    private final static String COMPONENT_BUTTON_1 = "Button_1";
    private final static String COMPONENT_BUTTON_2 = "Button_2";
    private final static String COMPONENT_BUTTON_3 = "Button_3";

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(GI_5133Type.GI_5133.getDefaultInstance()));
    }

    private final ButtonController button_0;
    private final ButtonController button_1;
    private final ButtonController button_2;
    private final ButtonController button_3;

    public GI_5142Controller(final String id, final Location location) throws RSBBindingException {
        super(id, location, GI_5133Type.GI_5133.newBuilder());

        builder.setId(id);
        this.button_0 = new ButtonController(COMPONENT_BUTTON_0, this, builder.getButton0Builder());
        this.button_1 = new ButtonController(COMPONENT_BUTTON_1, this, builder.getButton1Builder());
        this.button_2 = new ButtonController(COMPONENT_BUTTON_2, this, builder.getButton2Builder());
        this.button_3 = new ButtonController(COMPONENT_BUTTON_3, this, builder.getButton3Builder());
        this.register(button_0);
        this.register(button_1);
        this.register(button_2);
        this.register(button_3);
    }
    
    @Override
    protected void initHardwareMapping() throws NoSuchMethodException, SecurityException {
        halFunctionMapping.put(COMPONENT_BUTTON_0, getClass().getMethod("updateButton_0", OnOffType.class));
        halFunctionMapping.put(COMPONENT_BUTTON_1, getClass().getMethod("updateButton_1", OnOffType.class));
        halFunctionMapping.put(COMPONENT_BUTTON_2, getClass().getMethod("updateButton_2", OnOffType.class));
        halFunctionMapping.put(COMPONENT_BUTTON_3, getClass().getMethod("updateButton_3", OnOffType.class));
    }

    public void updateButton_0(OnOffType type) throws RSBBindingException {
        try {
            button_0.updateButtonState(ButtonStateTransformer.transform(type));
        } catch (RSBBindingException ex) {
            logger.error("Could not update button 0.", ex);
        }
    }
    public void updateButton_1(OnOffType type) throws RSBBindingException {
        try {
            button_1.updateButtonState(ButtonStateTransformer.transform(type));
        } catch (RSBBindingException ex) {
            logger.error("Could not update button 1.", ex);
        }
    }
    public void updateButton_2(OnOffType type) throws RSBBindingException {
        try {
            button_2.updateButtonState(ButtonStateTransformer.transform(type));
        } catch (RSBBindingException ex) {
            logger.error("Could not update button 2.", ex);
        }
    }
    public void updateButton_3(OnOffType type) throws RSBBindingException {
        try {
            button_3.updateButtonState(ButtonStateTransformer.transform(type));
        } catch (RSBBindingException ex) {
            logger.error("Could not update button 3.", ex);
        }
    }
}