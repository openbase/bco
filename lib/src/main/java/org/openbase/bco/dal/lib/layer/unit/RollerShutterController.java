package org.openbase.bco.dal.lib.layer.unit;

/*
 * #%L
 * BCO DAL Library
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.openbase.bco.dal.lib.layer.service.operation.BlindStateOperationService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.state.BlindStateType.BlindState;
import rst.domotic.unit.dal.RollerShutterDataType.RollerShutterData;
import rst.domotic.unit.UnitConfigType;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class RollerShutterController extends AbstractDALUnitController<RollerShutterData, RollerShutterData.Builder> implements RollerShutter {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(RollerShutterData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(BlindState.getDefaultInstance()));
    }

    private BlindStateOperationService blindStateService;

    public RollerShutterController(final UnitHost unitHost, final RollerShutterData.Builder builder) throws InstantiationException, CouldNotPerformException {
        super(RollerShutterController.class, unitHost, builder);
    }

    @Override
    public void init(UnitConfigType.UnitConfig config) throws InitializationException, InterruptedException {
        super.init(config);
        try {
            blindStateService = getServiceFactory().newShutterService(this);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    public void updateBlindStateProvider(final BlindState blindState) throws CouldNotPerformException {
        logger.debug("Apply blindState Update[" + blindState + "] for " + this + ".");

        try (ClosableDataBuilder<RollerShutterData.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setBlindState(blindState);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply blindState Update[" + blindState + "] for " + this + "!", ex);
        }
    }

    @Override
    public Future<Void> setBlindState(final BlindState blindState) throws CouldNotPerformException {
        logger.debug("Setting [" + getLabel() + "] to BlindState [" + blindState + "]");
        // stop before moving in any direction.
        switch (blindState.getMovementState()) {
            case DOWN:
            case UP:
                try {
                    blindStateService.setBlindState(blindState.toBuilder().setMovementState(BlindState.MovementState.STOP).build()).get(1000, TimeUnit.MILLISECONDS);
                } catch (ExecutionException | TimeoutException | InterruptedException ex) {
                    if (ex instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                    ExceptionPrinter.printHistory(new CouldNotPerformException("Could not stop before blind movement.", ex), logger, LogLevel.WARN);
                    // continue without stop
                }
        }
        return blindStateService.setBlindState(blindState);
    }

    @Override
    public BlindState getBlindState() throws NotAvailableException {
        try {
            return getData().getBlindState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("blindState", ex);
        }
    }
}
