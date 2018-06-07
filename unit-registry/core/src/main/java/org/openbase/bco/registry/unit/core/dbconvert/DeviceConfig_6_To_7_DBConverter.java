package org.openbase.bco.registry.unit.core.dbconvert;

import com.google.gson.JsonObject;
import org.openbase.jul.storage.registry.version.AbstractDBVersionConverter;
import org.openbase.jul.storage.registry.version.DBVersionControl;

import java.io.File;
import java.util.Map;

/**
 * Database converter which moves owner ids from the inventory state to
 * the permission config.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class DeviceConfig_6_To_7_DBConverter extends AbstractDBVersionConverter {

    public static final String INVENTORY_STATE_KEY = "inventory_state";
    public static final String OWNER_ID_KEY = "owner_id";
    public static final String PERMISSION_CONFIG_KEY = "permission_config";

    public DeviceConfig_6_To_7_DBConverter(DBVersionControl versionControl) {
        super(versionControl);
    }

    @Override
    public JsonObject upgrade(JsonObject outdatedDBEntry, Map<File, JsonObject> dbSnapshot) {
        final JsonObject deviceConfig = outdatedDBEntry.getAsJsonObject("device_config");
        if (deviceConfig.has(INVENTORY_STATE_KEY)) {
            final JsonObject inventoryState = deviceConfig.getAsJsonObject(INVENTORY_STATE_KEY);

            if (inventoryState.has(OWNER_ID_KEY)) {
                final String ownerId = inventoryState.get(OWNER_ID_KEY).getAsString();

                JsonObject permissionConfig;
                if (outdatedDBEntry.has(PERMISSION_CONFIG_KEY)) {
                    permissionConfig = outdatedDBEntry.getAsJsonObject(PERMISSION_CONFIG_KEY);
                } else {
                    permissionConfig = new JsonObject();
                    outdatedDBEntry.add(PERMISSION_CONFIG_KEY, permissionConfig);
                }

                permissionConfig.addProperty(OWNER_ID_KEY, ownerId);
            }
        }

        return outdatedDBEntry;
    }
}
