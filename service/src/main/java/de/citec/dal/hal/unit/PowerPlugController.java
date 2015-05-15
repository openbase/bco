/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.unit;

import de.citec.dal.hal.device.Device;
import de.citec.dal.hal.service.PowerService;
import de.citec.dal.hal.service.ServiceFactory;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.extension.protobuf.ClosableDataBuilder;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.PowerStateType.PowerState;
import rst.homeautomation.unit.PowerPlugType.PowerPlug;
import rst.homeautomation.unit.UnitConfigType;

/**
 *
 * @author mpohling
 */
public class PowerPlugController extends AbstractUnitController<PowerPlug, PowerPlug.Builder> implements PowerPlugInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerPlug.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerState.getDefaultInstance()));
    }

    private final PowerService powerService;

    public PowerPlugController(final UnitConfigType.UnitConfig config, final Device device, final PowerPlug.Builder builder) throws InstantiationException, CouldNotPerformException {
        this(config, device, builder, device.getDefaultServiceFactory());
    }

    public PowerPlugController(final UnitConfigType.UnitConfig config, final Device device, final PowerPlug.Builder builder, final ServiceFactory serviceFactory) throws InstantiationException, CouldNotPerformException {
        super(config, PowerPlugController.class, device, builder);
        this.powerService = serviceFactory.newPowerService(device, this);
    }

    public void updatePower(final PowerState.State value) throws CouldNotPerformException {
        logger.debug("Apply power Update[" + value + "] for " + this + ".");

        try (ClosableDataBuilder<PowerPlug.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().getPowerStateBuilder().setValue(value);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply power Update[" + value + "] for " + this + "!", ex);
        }
    }

    @Override
    public void setPower(final PowerState.State state) throws CouldNotPerformException {
        logger.debug("Setting [" + getLabel() + "] to Power [" + state.name() + "]");
        powerService.setPower(state);
    }

    @Override
    public PowerState getPower() throws NotAvailableException {
        try {
            return getData().getPowerState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("power", ex);
        }
    }
}
