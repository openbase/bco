package org.openbase.bco.app.openhab.manager;

import com.google.gson.JsonObject;
import com.google.protobuf.Message;
import org.openbase.bco.app.openhab.OpenHABRestCommunicator;
import org.openbase.bco.app.openhab.manager.transform.CommandTransformer;
import org.openbase.bco.app.openhab.registry.synchronizer.OpenHABItemHelper;
import org.openbase.bco.app.openhab.registry.synchronizer.OpenHABItemHelper.OpenHABItemNameMetaData;
import org.openbase.bco.dal.lib.layer.unit.UnitController;
import org.openbase.bco.dal.lib.layer.unit.UnitControllerRegistry;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        try {
            final UnitController unitController = unitControllerRegistry.get(Registries.getUnitRegistry().getUnitConfigByAlias(metaData.getAlias()).getId());
            final Message serviceData = CommandTransformer.getServiceData(state, metaData.getServiceType());

            if (serviceData == null) {
                // unsupported state for service, see CommandTransformer for details
                return;
            }

            unitController.applyDataUpdate(serviceData, metaData.getServiceType());
        } catch (NotAvailableException ex) {
            if (!unitControllerRegistry.isInitiallySynchronized()) {
                LOGGER.debug("ItemUpdate[" + itemName + "=" + state + "] skipped because controller registry was not ready yet!");
                return;
            }
            throw ex;
        }
    }
}
