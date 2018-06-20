package org.openbase.bco.app.cloud.connector.mapping.service;

import org.openbase.bco.app.cloud.connector.mapping.lib.Mode;
import org.openbase.bco.app.cloud.connector.mapping.lib.Setting;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.PowerConsumptionStateType.PowerConsumptionState;

import java.util.Arrays;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class PowerConsumptionModeMapper extends AbstractSingleModeServiceStateProviderMapper<PowerConsumptionState> {

    private final Mode mode;
    private final Setting low, medium, high;

    public PowerConsumptionModeMapper() {
        super(ServiceType.POWER_CONSUMPTION_STATE_SERVICE);

        this.mode = new Mode("consumption", true, "verbrauch", "stromverbrauch", "energieverbrauch");
        this.low = new Setting("low", "niedrig", "gering");
        this.medium = new Setting("medium", "mittel", "normal");
        this.high = new Setting("high", "hoch", "stark");
        this.mode.getSettingList().addAll(Arrays.asList(low, medium, high));
    }

    @Override
    public Mode getMode() {
        return mode;
    }

    @Override
    public String getSetting(final PowerConsumptionState powerConsumptionState) {
        final double consumption = powerConsumptionState.getConsumption();
        if (consumption < 100) {
            return low.getName();
        } else if (consumption < 500) {
            return medium.getName();
        } else {
            return high.getName();
        }
    }
}
