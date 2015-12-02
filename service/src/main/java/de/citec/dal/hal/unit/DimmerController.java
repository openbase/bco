/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.unit;

import de.citec.dal.hal.device.Device;
import de.citec.dal.hal.service.DimService;
import de.citec.dal.hal.service.PowerService;
import de.citec.dal.hal.service.ServiceFactory;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.extension.protobuf.ClosableDataBuilder;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.PowerStateType.PowerState;
import rst.homeautomation.unit.DimmerType.Dimmer;
import rst.homeautomation.unit.UnitConfigType;

/**
 *
 * @author thuxohl
 */
public class DimmerController extends AbstractUnitController<Dimmer, Dimmer.Builder> implements DimmerInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(Dimmer.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerState.getDefaultInstance()));
    }

    private final PowerService powerService;
    private final DimService dimmService;

    public DimmerController(final UnitConfigType.UnitConfig config, Device device, Dimmer.Builder builder) throws de.citec.jul.exception.InstantiationException, CouldNotPerformException {
        this(config, device, builder, device.getServiceFactory());
    }

    public DimmerController(final UnitConfigType.UnitConfig config, Device device, Dimmer.Builder builder, final ServiceFactory serviceFactory) throws de.citec.jul.exception.InstantiationException, CouldNotPerformException {
        super(config, DimmerController.class, device, builder);
        this.powerService = serviceFactory.newPowerService(device, this);
        this.dimmService = serviceFactory.newDimmService(device, this);
    }

    public void updatePower(final PowerState.State value) throws CouldNotPerformException {
        logger.debug("Apply power Update[" + value + "] for " + this + ".");

        try (ClosableDataBuilder<Dimmer.Builder> dataBuilder = getDataBuilder(this)) {
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

    public void updateDim(final Double value) throws CouldNotPerformException {
        logger.debug("Apply dim Update[" + value + "] for " + this + ".");

        try (ClosableDataBuilder<Dimmer.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setValue(value);
            if(value == 0) {
                dataBuilder.getInternalBuilder().getPowerStateBuilder().setValue(PowerState.State.OFF);
            } else {
                dataBuilder.getInternalBuilder().getPowerStateBuilder().setValue(PowerState.State.ON);
            }
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply dim Update[" + value + "] for " + this + "!", ex);
        }
    }

    @Override
    public void setDim(Double dimm) throws CouldNotPerformException {
        dimmService.setDim(dimm);
    }

    @Override
    public Double getDim() throws NotAvailableException {
        try {
            return getData().getValue();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("dim", ex);
        }
    }
}
