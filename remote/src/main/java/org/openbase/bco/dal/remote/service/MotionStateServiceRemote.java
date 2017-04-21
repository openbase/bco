package org.openbase.bco.dal.remote.service;

/*
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import org.openbase.bco.dal.lib.layer.service.collection.MotionStateProviderServiceCollection;
import org.openbase.bco.dal.lib.layer.service.provider.MotionStateProviderService;
import org.openbase.bco.dal.remote.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.MotionStateType.MotionState;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.timing.TimestampType.Timestamp;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class MotionStateServiceRemote extends AbstractServiceRemote<MotionStateProviderService, MotionState> implements MotionStateProviderServiceCollection {

    public MotionStateServiceRemote() {
        super(ServiceType.MOTION_STATE_SERVICE, MotionState.class);
    }

    public Collection<MotionStateProviderService> getMotionStateProviderServices() {
        return getServices();
    }

    /**
     * {@inheritDoc}
     * Computes the motion state as motion if at least one underlying services replies with motion and else no motion.
     * Additionally the last motion timestamp is set as the latest of the underlying services.
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected MotionState computeServiceState() throws CouldNotPerformException {
        return getMotionState(UnitType.UNKNOWN);
    }

    @Override
    public MotionState getMotionState() throws NotAvailableException {
        return getServiceState();
    }

    @Override
    public MotionState getMotionState(UnitType unitType) throws NotAvailableException {
        MotionState.State motionValue = MotionState.State.NO_MOTION;
        long lastMotion = 0;
        long timestamp = 0;
        for (MotionStateProviderService service : getServices(unitType)) {
            if (!((UnitRemote) service).isDataAvailable()) {
                continue;
            }

            MotionState motionState = service.getMotionState();
            if (motionState.getValue() == MotionState.State.MOTION) {
                motionValue = MotionState.State.MOTION;
            }

            if (motionState.hasLastMotion() && motionState.getLastMotion().getTime() > lastMotion) {
                lastMotion = motionState.getLastMotion().getTime();
            }

            timestamp = Math.max(timestamp, motionState.getTimestamp().getTime());
        }
        return TimestampProcessor.updateTimestamp(timestamp, MotionState.newBuilder().setValue(motionValue).setLastMotion(Timestamp.newBuilder().setTime(lastMotion)), TimeUnit.MICROSECONDS, logger).build();
    }
}
