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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import org.openbase.bco.dal.lib.layer.service.operation.PowerStateOperationService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
import org.openbase.jul.schedule.FutureProcessor;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.unit.dal.LightDataType.LightData;
import rst.domotic.unit.UnitConfigType;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class LightController extends AbstractDALUnitController<LightData, LightData.Builder> implements Light {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(LightData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerState.getDefaultInstance()));
    }

    private PowerStateOperationService powerService;

    public LightController(final UnitHost unitHost, final LightData.Builder builder) throws InstantiationException, CouldNotPerformException {
        super(LightController.class, unitHost, builder);
    }

    @Override
    public void init(UnitConfigType.UnitConfig config) throws InitializationException, InterruptedException {
        super.init(config);
        try {
            this.powerService = getServiceFactory().newPowerService(this);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    public void updatePowerStateProvider(final PowerState powerState) throws CouldNotPerformException {
        logger.debug("Apply powerState Update[" + powerState + "] for " + this + ".");
        try (ClosableDataBuilder<LightData.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setPowerState(powerState);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply powerState Update[" + powerState + "] for " + this + "!", ex);
        }
    }

    @Override
    public Future<Void> setPowerState(final PowerState state) throws CouldNotPerformException {
        logger.debug("Setting [" + getLabel() + "] to PowerState [" + state + "]");
        try {
            verifyOperationServiceStateValue(state.getValue());
        } catch(VerificationFailedException ex) {
            return FutureProcessor.canceledFuture(Void.class, ex);
        }
        return powerService.setPowerState(state);
    }

    @Override
    public PowerState getPowerState() throws NotAvailableException {
        try {
            return getData().getPowerState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("powerState", ex);
        }
    }
}
