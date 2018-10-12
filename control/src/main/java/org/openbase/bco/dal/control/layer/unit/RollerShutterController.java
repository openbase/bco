package org.openbase.bco.dal.control.layer.unit;

/*
 * #%L
 * BCO DAL Control
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
import java.util.concurrent.Future;

import org.openbase.bco.dal.lib.layer.unit.RollerShutter;
import org.openbase.bco.dal.lib.layer.unit.HostUnitController;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.state.BlindStateType.BlindState;
import rst.domotic.unit.dal.RollerShutterDataType.RollerShutterData;

import static rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.BLIND_STATE_SERVICE;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class RollerShutterController extends AbstractDALUnitController<RollerShutterData, RollerShutterData.Builder> implements RollerShutter {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(RollerShutterData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(BlindState.getDefaultInstance()));
    }

    public RollerShutterController(final HostUnitController hostUnitController, final RollerShutterData.Builder builder) throws InstantiationException {
        super(RollerShutterController.class, hostUnitController, builder);
    }

    @Override
    public Future<ActionDescription> setBlindState(final BlindState state) throws CouldNotPerformException {
//        logger.debug("Setting [" + getLabel() + "] to BlindState [" + state + "]");
//
//        try {
//            Services.verifyServiceState(state);
//        } catch (VerificationFailedException ex) {
//            return FutureProcessor.canceledFuture(ActionDescription.class, ex);
//        }
//
//        // stop before moving in any direction.
//        switch (state.getValue()) {
//            case DOWN:
//            case UP:
//                try {
//                    blindStateService.setBlindState(state.toBuilder().setValue(BlindState.State.STOP).build()).get(1000, TimeUnit.MILLISECONDS);
//                } catch (ExecutionException | TimeoutException | InterruptedException ex) {
//                    if (ex instanceof InterruptedException) {
//                        Thread.currentThread().interrupt();
//                    }
//                    ExceptionPrinter.printHistory(new CouldNotPerformException("Could not stop before inverse blind movement.", ex), logger, LogLevel.WARN);
//                    // continue without stop
//                }
//        }
//        return blindStateService.setBlindState(state);
        return applyUnauthorizedAction(state, BLIND_STATE_SERVICE);
    }
}
