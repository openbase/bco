package org.openbase.bco.app.openhab.manager.transform;

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.openbase.jul.exception.CouldNotTransformException;
import org.openbase.jul.exception.TypeNotSupportedException;
import rst.domotic.binding.openhab.OnOffHolderType;
import rst.domotic.state.StandbyStateType.StandbyState;
import rst.domotic.state.StandbyStateType.StandbyState.State;

public class StandbyStateTransformer {

    public static StandbyState transform(OnOffType onOffType) throws CouldNotTransformException {
        switch (onOffType) {
            case OFF:
                return StandbyState.newBuilder().setValue(State.RUNNING).build();
            case ON:
                return StandbyState.newBuilder().setValue(State.STANDBY).build();
            default:
                throw new CouldNotTransformException("Could not transform " + OnOffHolderType.OnOffHolder.OnOff.class.getSimpleName() + "[" + onOffType.name() + "] is unknown!");
        }
    }

    public static OnOffType transform(StandbyState standbyState) throws TypeNotSupportedException, CouldNotTransformException {
        switch (standbyState.getValue()) {
            case RUNNING:
                return OnOffType.OFF;
            case STANDBY:
                return OnOffType.ON;
            case UNKNOWN:
                throw new TypeNotSupportedException(standbyState, OnOffType.class);
            default:
                throw new CouldNotTransformException("Could not transform " + StandbyState.State.class.getSimpleName() + "[" + standbyState.getValue().name() + "] is unknown!");
        }
    }
}
