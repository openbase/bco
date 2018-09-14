package org.openbase.bco.dal.remote.printer;

/*-
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.bco.dal.remote.unit.CustomUnitPool;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import org.openbase.jul.iface.DefaultInitializable;
import org.openbase.jul.pattern.Filter;
import org.openbase.jul.pattern.Observer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.service.ServiceDescriptionType.ServiceDescription;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.vision.ColorType.Color;
import rst.vision.HSBColorType.HSBColor;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

public class UnitStatePrinter implements DefaultInitializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnitStatePrinter.class);

    private final CustomUnitPool customUnitPool;
    private final Observer<Unit, Message> unitStateObserver;
    private final PrintStream printStream;

    public UnitStatePrinter(final PrintStream printStream, final Filter<UnitConfig>... filters) throws InstantiationException {
        try {
            this.printStream = printStream;
            this.customUnitPool = new CustomUnitPool(filters);
            this.unitStateObserver = (source, data) -> print(source, data);
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
            final List<String> states = extractServiceStates(data, serviceType);
            if (!states.isEmpty()) {
                printStream.println("===========================================================================================================");
            }
            for (String extractServiceState : states) {
                printStream.println("unit(" + unit.getUnitType().name().toLowerCase() + ", " + unit.getId() + ", " + extractServiceState + ").");
            }
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not print " + serviceType.name() + " of " + unit, ex, LOGGER);
        }
    }

    private List<String> extractServiceStates(Object serviceProvider, ServiceType serviceType) throws CouldNotPerformException {

        Message serviceState = Services.invokeProviderServiceMethod(serviceType, serviceProvider);

        final List<String> states = new ArrayList<>();

        for (Entry<FieldDescriptor, Object> entry : serviceState.getAllFields().entrySet()) {


            if (!entry.getKey().isRepeated() && !serviceState.hasField(entry.getKey()) || entry.getValue() == null) {
                continue;
            }

            String stateName = entry.getKey().getName();
            String stateValue = entry.getValue().toString();
            if (stateName == null || entry.getValue() == null) {
                continue;
            }

            String timestamp;
            try {
                timestamp = Long.toString(TimestampProcessor.getTimestamp(serviceState, TimeUnit.MILLISECONDS));
            } catch (NotAvailableException ex) {
                timestamp = "?";
            }

            switch (stateValue) {
                case "":
                case "NaN":
                    continue;
                default:
                    break;
            }

            switch (stateName) {
                case "":
                case "last_value_occurrence":
                case "timestamp":
                case "responsible_action":
                    continue;
                case "color":
                    final HSBColor hsbColor = ((Color) entry.getValue()).getHsbColor();
                    stateValue = hsbColor.getHue() + " " + hsbColor.getSaturation() + " " + hsbColor.getBrightness();
                    break;
                case "value":
                    stateName = "state";
                    break;
            }
            states.add(serviceType.name().toLowerCase() + ", " + timestamp + ", " + stateName.toLowerCase() + ", " + stateValue.toLowerCase());
        }
        return states;
    }
}
