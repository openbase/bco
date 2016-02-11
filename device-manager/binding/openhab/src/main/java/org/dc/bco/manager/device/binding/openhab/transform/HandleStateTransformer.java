/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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

import org.dc.jul.exception.CouldNotTransformException;
import org.dc.jul.processing.StringProcessor;
import rst.homeautomation.state.HandleStateType.HandleState;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 */
public class HandleStateTransformer {

    public static HandleState.State transform(final String value) throws CouldNotTransformException {
        try {
            return HandleState.State.valueOf(StringProcessor.transformToUpperCase(value));
        } catch (Exception ex) {
            throw new CouldNotTransformException("Could not transform " + String.class.getName() + "! " + String.class.getSimpleName() + "[" + value + "] is not a valid " + HandleState.State.class.getSimpleName() + "!", ex);
        }
    }

    public static String transform(final HandleState.State value) throws CouldNotTransformException {

        try {
            return StringProcessor.transformToUpperCase(value.name());
        } catch (Exception ex) {
            throw new CouldNotTransformException("Could not transform " + HandleState.State.class.getName() + "[" + value + "] to " + String.class.getSimpleName() + "!", ex);
        }
    }
}
