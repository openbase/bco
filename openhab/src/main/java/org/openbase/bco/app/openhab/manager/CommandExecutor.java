package org.openbase.bco.app.openhab.manager;

import com.google.gson.JsonObject;
import org.eclipse.smarthome.core.library.types.HSBType;
import org.openbase.bco.app.openhab.OpenHABRestCommunicator;
import org.openbase.bco.app.openhab.manager.transform.OpenHABColorStateTransformer;
import org.openbase.bco.app.openhab.registry.synchronizer.OpenHABItemHelper;
import org.openbase.bco.app.openhab.registry.synchronizer.OpenHABItemHelper.OpenHABItemNameMetaData;
import org.openbase.bco.dal.lib.layer.unit.UnitController;
import org.openbase.bco.dal.lib.layer.unit.UnitControllerRegistry;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.state.ColorStateType.ColorState;

public class CommandExecutor implements Observer<JsonObject> {

    public static final String PAYLOAD_KEY = "payload";

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandExecutor.class);

    private final UnitControllerRegistry<?, ?> unitControllerRegistry;

    public CommandExecutor(final UnitControllerRegistry unitControllerRegistry) {
        this.unitControllerRegistry = unitControllerRegistry;
    }

    @Override
    public void update(Observable<JsonObject> observable, JsonObject payload) {
        // extract item name from topic
        final String topic = payload.get(OpenHABRestCommunicator.TOPIC_KEY).getAsString();
        // topic structure: smarthome/items/{itemName}/command
        final String itemName = topic.split(OpenHABRestCommunicator.TOPIC_SEPERATOR)[2];

        // extract payload
        final String state = payload.get(PAYLOAD_KEY).getAsString();

        try {
            applyStateUpdate(itemName, state);
        } catch (CouldNotPerformException ex) {
            LOGGER.warn("Could not apply state update[" + state + "] for item[" + itemName + "]", ex);
        }
    }

    public void applyStateUpdate(final String itemName, final String state) throws CouldNotPerformException {
        final OpenHABItemNameMetaData metaData = OpenHABItemHelper.getMetaData(itemName);
        final UnitController unitController = unitControllerRegistry.get(Registries.getUnitRegistry().getUnitConfigByAlias(metaData.getAlias()).getId());

        switch (metaData.getServiceType()) {
            case COLOR_STATE_SERVICE:
                ColorState colorState = OpenHABColorStateTransformer.transform(HSBType.valueOf(state));
                unitController.applyDataUpdate(TimestampProcessor.updateTimestampWithCurrentTime(colorState), metaData.getServiceType());
                break;
            case POWER_STATE_SERVICE:
                break;
            case BRIGHTNESS_STATE_SERVICE:
                break;
        }
    }
}
