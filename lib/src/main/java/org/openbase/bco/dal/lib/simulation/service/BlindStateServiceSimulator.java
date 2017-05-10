package org.openbase.bco.dal.lib.simulation.service;

import org.openbase.bco.dal.lib.layer.unit.UnitController;
import org.openbase.jul.exception.NotAvailableException;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.BlindStateType.BlindState;

/**
 * Custom service simulator.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class BlindStateServiceSimulator extends AbstractScheduledServiceSimulator<BlindState> {

    public static final int MIN_OPENING_RATIO = 0;
    public static final int MAX_OPENING_RATIO = 100;
    public static final int OPENING_RATIO_STEP = 10;

    private final BlindState.Builder simulatedBlindState;

    /**
     * Creates a new custom service simulator.
     *
     * @param unitController the unit to simulate.
     */
    public BlindStateServiceSimulator(UnitController unitController) {
        super(unitController, ServiceType.BLIND_STATE_SERVICE);
        this.simulatedBlindState = BlindState.newBuilder();
        this.simulatedBlindState.setMovementState(BlindState.MovementState.STOP);
        this.simulatedBlindState.setOpeningRatio(100);
    }

    private BlindState getSimulatedBlindState() {
        switch (simulatedBlindState.getMovementState()) {
            case STOP:
                if (simulatedBlindState.getOpeningRatio() == MAX_OPENING_RATIO) {
                    simulatedBlindState.setMovementState(BlindState.MovementState.DOWN);
                    simulatedBlindState.setOpeningRatio(MAX_OPENING_RATIO - OPENING_RATIO_STEP);
                } else {
                    simulatedBlindState.setMovementState(BlindState.MovementState.UP);
                    simulatedBlindState.setOpeningRatio(MIN_OPENING_RATIO + OPENING_RATIO_STEP);
                }
                break;
            case DOWN:
                if (simulatedBlindState.getOpeningRatio() > MIN_OPENING_RATIO) {
                    simulatedBlindState.setOpeningRatio(simulatedBlindState.getOpeningRatio() - OPENING_RATIO_STEP);
                } else {
                    simulatedBlindState.setMovementState(BlindState.MovementState.STOP);
                    simulatedBlindState.setOpeningRatio(MIN_OPENING_RATIO);
                }
                break;
            case UP:
                if (simulatedBlindState.getOpeningRatio() < MAX_OPENING_RATIO) {
                    simulatedBlindState.setOpeningRatio(simulatedBlindState.getOpeningRatio() + OPENING_RATIO_STEP);
                } else {
                    simulatedBlindState.setMovementState(BlindState.MovementState.STOP);
                    simulatedBlindState.setOpeningRatio(MAX_OPENING_RATIO);
                }
                break;
            default:
                simulatedBlindState.setMovementState(BlindState.MovementState.STOP);
                simulatedBlindState.setOpeningRatio(MAX_OPENING_RATIO);
                break;
        }
        return simulatedBlindState.build();
    }

    /**
     * {@inheritDoc }
     *
     * @return {@inheritDoc }
     * @throws NotAvailableException {@inheritDoc }
     */
    @Override
    protected BlindState getNextServiceState() throws NotAvailableException {
        return getSimulatedBlindState();
    }
}
