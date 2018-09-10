package org.openbase.bco.dal.remote.printer;

import com.google.protobuf.Message;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.bco.dal.remote.unit.CustomUnitPool;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.iface.DefaultInitializable;
import org.openbase.jul.pattern.Observer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.service.ServiceDescriptionType.ServiceDescription;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;

public class UnitStateLogger implements DefaultInitializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnitStateLogger.class);

    private final CustomUnitPool customUnitPool;
    private final Observer<Message> unitStateObserver;

    public UnitStateLogger() throws InstantiationException {
        try {
            this.customUnitPool = new CustomUnitPool();
            this.unitStateObserver = (source, data) -> {
                print((Unit) source, data);
            };
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    public void init() throws InitializationException, InterruptedException {
        try {
            customUnitPool.init();
            customUnitPool.addObserver(unitStateObserver);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    private void print(Unit unit, Message data) {
        try {
            for (ServiceDescription serviceDescription : unit.getUnitTemplate().getServiceDescriptionList()) {
                print(unit, serviceDescription.getServiceType(), data);
            }
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not print " + unit, ex, LOGGER);
        }
    }

    private void print(Unit unit, ServiceType serviceType, Message data) {
        try {
            System.out.println("unit(" + unit.getUnitType().name().toLowerCase() + ", " + unit.getId() + ", " + Services.invokeProviderServiceMethod(serviceType, unit) + ").");
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not print " + serviceType.name() + " of " + unit, ex, LOGGER);
        }
    }
}
