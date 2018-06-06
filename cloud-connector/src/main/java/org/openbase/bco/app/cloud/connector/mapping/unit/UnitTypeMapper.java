package org.openbase.bco.app.cloud.connector.mapping.unit;

import com.google.gson.JsonObject;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public interface UnitTypeMapper<UR extends UnitRemote> {

    void map(final UR unitRemote, final JsonObject jsonObject) throws CouldNotPerformException;

    static UnitTypeMapper getByType(final UnitType unitType) {
        switch (unitType) {
            case TEMPERATURE_CONTROLLER:
                return new TemperatureControllerUnitTypeMapper();
            default:
                return new DefaultUnitTypeMapper();
        }
    }
}
