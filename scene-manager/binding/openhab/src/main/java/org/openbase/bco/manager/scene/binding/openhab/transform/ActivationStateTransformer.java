package org.openbase.bco.manager.scene.binding.openhab.transform;

/*
 * #%L
 * BCO Manager Scene Binding OpenHAB
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

import org.openbase.jul.exception.CouldNotTransformException;
import org.openbase.jul.exception.TypeNotSupportedException;
import rst.domotic.binding.openhab.OnOffHolderType;
import rst.domotic.state.ActivationStateType.ActivationState;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ActivationStateTransformer {

    public static ActivationState transform(OnOffHolderType.OnOffHolder.OnOff onOffType) throws CouldNotTransformException {
        switch (onOffType) {
            case OFF:
                return ActivationState.newBuilder().setValue(ActivationState.State.DEACTIVE).build();
            case ON:
                return ActivationState.newBuilder().setValue(ActivationState.State.ACTIVE).build();
            default:
                throw new CouldNotTransformException("Could not transform " + OnOffHolderType.OnOffHolder.OnOff.class.getName() + "! " + OnOffHolderType.OnOffHolder.OnOff.class.getSimpleName() + "[" + onOffType.name() + "] is unknown!");
        }
    }

    public static OnOffHolderType.OnOffHolder transform(ActivationState.State activationState) throws TypeNotSupportedException, CouldNotTransformException {
        switch (activationState) {
            case DEACTIVE:
                return OnOffHolderType.OnOffHolder.newBuilder().setState(OnOffHolderType.OnOffHolder.OnOff.OFF).build();
            case ACTIVE:
                return OnOffHolderType.OnOffHolder.newBuilder().setState(OnOffHolderType.OnOffHolder.OnOff.ON).build();
            case UNKNOWN:
                throw new TypeNotSupportedException(activationState, OnOffHolderType.OnOffHolder.OnOff.class);
            default:
                throw new CouldNotTransformException("Could not transform " + ActivationState.State.class.getName() + "! " + ActivationState.State.class.getSimpleName() + "[" + activationState.name() + "] is unknown!");
        }
    }
}
