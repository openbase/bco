package de.citec.dm.core.registry;

import com.google.gson.JsonObject;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 *
 * This converter transforms the outdated db entry into a new db version entry.
 */
public interface DBVersionConverter {

    public JsonObject upgrade(JsonObject outdatedDBEntry);
}
