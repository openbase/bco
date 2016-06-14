package org.openbase.bco.dal.lib.layer.service.collection;

/*
 * #%L
 * DAL Library
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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
import java.util.concurrent.Future;
import org.openbase.bco.dal.lib.layer.service.operation.PowerOperationService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.schedule.GlobalExecutionService;
import rst.homeautomation.state.PowerStateType.PowerState;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public interface PowerStateOperationServiceCollection extends PowerOperationService {

    @Override
    default public Future<Void> setPower(final PowerState state) throws CouldNotPerformException {
        return GlobalExecutionService.allOf((PowerOperationService input) -> input.setPower(state), getPowerStateOperationServices());
    }

    /**
     * Returns on if at least one of the power services is on and else off.
     *
     * @return
     * @throws NotAvailableException
     */
    @Override
    default public PowerState getPower() throws NotAvailableException {
        try {
            for (PowerOperationService service : getPowerStateOperationServices()) {
                if (service.getPower().getValue() == PowerState.State.ON) {
                    return PowerState.newBuilder().setValue(PowerState.State.ON).build();
                }
            }
            return PowerState.newBuilder().setValue(PowerState.State.OFF).build();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("PowerState", ex);
        }
    }

    public Collection<PowerOperationService> getPowerStateOperationServices() throws CouldNotPerformException;
}
