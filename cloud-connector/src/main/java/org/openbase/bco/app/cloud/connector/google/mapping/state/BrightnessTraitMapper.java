package org.openbase.bco.app.cloud.connector.google.mapping.state;

import com.google.gson.JsonObject;
import org.openbase.jul.exception.CouldNotPerformException;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.BrightnessStateType.BrightnessState;
import rst.domotic.state.BrightnessStateType.BrightnessState.DataUnit;

public class BrightnessTraitMapper implements TraitMapper<BrightnessState> {

    public static final String BRIGHTNESS_TRAIT_KEY = "brightness";

    @Override
    public BrightnessState map(JsonObject jsonObject) throws CouldNotPerformException {
        if (!jsonObject.has(BRIGHTNESS_TRAIT_KEY)) {
            throw new CouldNotPerformException("Could not map from jsonObject[" + jsonObject.toString() + "] to [" + BrightnessState.class.getSimpleName() + "]. Attribute[" + BRIGHTNESS_TRAIT_KEY + "] is missing");
        }

        try {
            final int brightness = jsonObject.get(BRIGHTNESS_TRAIT_KEY).getAsInt();
            return BrightnessState.newBuilder().setBrightnessDataUnit(DataUnit.PERCENT).setBrightness(brightness).build();
        } catch (ClassCastException | IllegalStateException ex) {
            // thrown if it is not an int
            throw new CouldNotPerformException("Could not map from jsonObject[" + jsonObject.toString() + "] to [" + BrightnessState.class.getSimpleName() + "]. Attribute[" + BRIGHTNESS_TRAIT_KEY + "] is not an integer");
        }
    }

    @Override
    public void map(BrightnessState brightnessState, JsonObject jsonObject) {
        jsonObject.addProperty(BRIGHTNESS_TRAIT_KEY, (int) brightnessState.getBrightness());
    }

    @Override
    public ServiceType getServiceType() {
        return ServiceType.BRIGHTNESS_STATE_SERVICE;
    }
}
