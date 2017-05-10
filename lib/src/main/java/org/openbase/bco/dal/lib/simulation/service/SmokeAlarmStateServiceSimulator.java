package org.openbase.bco.dal.lib.simulation.service;

import org.openbase.bco.dal.lib.layer.unit.UnitController;
import org.openbase.jul.exception.NotAvailableException;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.AlarmStateType.AlarmState;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class SmokeAlarmStateServiceSimulator extends AbstractScheduledServiceSimulator<AlarmState> {

    public static final int NO_ALARM_ITERATIONS = 5;

    private final AlarmState.Builder simulatedAlarmState;
    private int counter;

    /**
     * Creates a new custom unit simulator.
     *
     * @param unitController the unit to simulate.
     */
    public SmokeAlarmStateServiceSimulator(UnitController unitController) {
        super(unitController, ServiceType.SMOKE_ALARM_STATE_SERVICE);
        this.simulatedAlarmState = AlarmState.newBuilder();
        this.simulatedAlarmState.setValue(AlarmState.State.NO_ALARM);
        this.counter = 0;
    }

    private AlarmState getSimulatedAlarmState() {
        switch (simulatedAlarmState.getValue()) {
            case ALARM:
                simulatedAlarmState.setValue(AlarmState.State.NO_ALARM);
                break;
            case NO_ALARM:
                if (counter < NO_ALARM_ITERATIONS) {
                    counter++;
                } else {
                    counter = 0;
                    simulatedAlarmState.setValue(AlarmState.State.ALARM);
                }
                break;
            default:
                simulatedAlarmState.setValue(AlarmState.State.NO_ALARM);
                break;
        }
        return simulatedAlarmState.build();
    }

    /**
     * {@inheritDoc }
     *
     * @return {@inheritDoc }
     * @throws NotAvailableException {@inheritDoc }
     */
    @Override
    protected AlarmState getNextServiceState() throws NotAvailableException {
        return getSimulatedAlarmState();
    }
}
