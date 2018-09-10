package org.openbase.bco.dal.lib.layer.unit;

import org.openbase.jul.pattern.Filter;
import rst.domotic.state.EnablingStateType.EnablingState.State;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 * A collection of useful unit filters.
 */
public class UnitFilters {

    /**
     * Filters all units which are disabled and let enabled ones pass.
     */
    public static final Filter<UnitConfig> DISABELED_UNIT_FILTER = unitConfig -> unitConfig.getEnablingState().getValue() != State.ENABLED;
}
