/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.data.transform;

import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.exception.TypeNotSupportedException;
import org.openhab.core.library.types.OnOffType;
import rst.homeautomation.states.ClickType;

/**
 *
 * @author mpohling
 */
public class ButtonStateTransformer {

    public static ClickType.Click.ClickState transform(OnOffType onOffType) throws RSBBindingException {
        switch (onOffType) {
            case OFF:
                return ClickType.Click.ClickState.RELEASED;
            case ON:
                return ClickType.Click.ClickState.CLICKED;
            default:
                throw new RSBBindingException("Could not transform " + OnOffType.class.getName() + "! " + OnOffType.class.getSimpleName() + "[" + onOffType.name() + "] is unknown!");
        }
    }

    public static OnOffType transform(ClickType.Click.ClickState clickState) throws TypeNotSupportedException, RSBBindingException {
        switch (clickState) {
            case RELEASED:
                return OnOffType.OFF;
            case CLICKED:
                return OnOffType.ON;
            case UNKNOWN:
                throw new TypeNotSupportedException(clickState, OnOffType.class);
            default:
                throw new RSBBindingException("Could not transform " + ClickType.Click.ClickState.class.getName() + "! " + ClickType.Click.ClickState.class.getSimpleName() + "[" + clickState.name() + "] is unknown!");
        }
    }
}
