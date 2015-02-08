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
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.VerificationFailedException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.devices.gira.GI_5133Type;
import rst.homeautomation.openhab.OnOffHolderType.OnOffHolder.OnOff;

/**
 *
 * @author mpohling
 */
public class GI_5133Controller extends AbstractOpenHABDeviceController<GI_5133Type.GI_5133, GI_5133Type.GI_5133.Builder> {

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

    public GI_5133Controller(final String id, final String label, final String[] unitLabel, final Location location) throws VerificationFailedException, InstantiationException {
        super(id, label, location, GI_5133Type.GI_5133.newBuilder());

        data.setId(id);
        this.button_0 = new ButtonController(unitLabel[0], this, data.getButton0Builder());
        this.button_1 = new ButtonController(unitLabel[1], this, data.getButton1Builder());
        this.button_2 = new ButtonController(unitLabel[2], this, data.getButton2Builder());
        this.button_3 = new ButtonController( unitLabel[3], this, data.getButton3Builder());
        this.button_4 = new ButtonController(unitLabel[4], this, data.getButton4Builder());
        this.button_5 = new ButtonController(unitLabel[5], this, data.getButton5Builder());
        this.registerUnit(button_0);
        this.registerUnit(button_1);
        this.registerUnit(button_2);
        this.registerUnit(button_3);
        this.registerUnit(button_4);
        this.registerUnit(button_5);
    }

    public void updateButton_0(OnOff type) throws CouldNotPerformException {
        try {
            logger.debug("Try to update button 0");
            button_0.updateButtonState(ButtonStateTransformer.transform(type));
        } catch (RSBBindingException ex) {
            throw new CouldNotPerformException("Could not updateButton_0!", ex);
        }
    }

    public void updateButton_1(OnOff type) throws CouldNotPerformException {
        try {
            button_1.updateButtonState(ButtonStateTransformer.transform(type));
        } catch (RSBBindingException ex) {
            throw new CouldNotPerformException("Could not updateButton_1!", ex);
        }
    }

    public void updateButton_2(OnOff type) throws CouldNotPerformException {
        try {
            button_2.updateButtonState(ButtonStateTransformer.transform(type));
        } catch (RSBBindingException ex) {
            throw new CouldNotPerformException("Could not updateButton_2!", ex);
        }
    }

    public void updateButton_3(OnOff type) throws CouldNotPerformException {
        try {
            button_3.updateButtonState(ButtonStateTransformer.transform(type));
        } catch (RSBBindingException ex) {
            throw new CouldNotPerformException("Could not updateButton_3!", ex);
        }
    }

    public void updateButton_4(OnOff type) throws CouldNotPerformException {
        try {
            button_4.updateButtonState(ButtonStateTransformer.transform(type));
        } catch (RSBBindingException ex) {
            throw new CouldNotPerformException("Could not updateButton_4!", ex);
        }
    }

    public void updateButton_5(OnOff type) throws CouldNotPerformException {
        try {
            button_5.updateButtonState(ButtonStateTransformer.transform(type));
        } catch (RSBBindingException ex) {
            throw new CouldNotPerformException("Could not updateButton_5!", ex);
        }
    }
}
