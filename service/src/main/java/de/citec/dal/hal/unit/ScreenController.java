/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.unit;


import de.citec.dal.hal.service.PowerService;
import de.citec.dal.hal.service.StandbyService;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.protobuf.ClosableDataBuilder;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.PowerStateType.PowerState;
import rst.homeautomation.state.StandbyStateType.StandbyState;
import rst.homeautomation.unit.ScreenType.Screen;
import rst.homeautomation.unit.UnitConfigType;

/**
 *
 * @author mpohling
 */
public class ScreenController extends AbstractUnitController<Screen, Screen.Builder> implements ScreenInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(Screen.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(StandbyState.getDefaultInstance()));
    }

    private final PowerService powerService;
    private final StandbyService standbyService;

    public ScreenController(final UnitConfigType.UnitConfig config, final UnitHost unitHost, final Screen.Builder builder) throws InstantiationException, CouldNotPerformException {
        super(config, ScreenController.class, unitHost, builder);
        this.powerService = getServiceFactory().newPowerService(this);
        this.standbyService = getServiceFactory().newStandbyService(this);
    }

    public void updatePower(final PowerState.State value) throws CouldNotPerformException {
        logger.debug("Apply power Update[" + value + "] for " + this + ".");

        try (ClosableDataBuilder<Screen.Builder> dataBuilder = getDataBuilder(this)) {
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

    @Override
    public void setStandby(StandbyState.State state) throws CouldNotPerformException {
        logger.debug("Setting [" + getLabel() + "] to Power [" + state.name() + "]");
        standbyService.setStandby(state);
    }

    @Override
    public StandbyState getStandby() throws CouldNotPerformException {
        try {
            return getData().getStandbyState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("standby", ex);
        }
    }
}
