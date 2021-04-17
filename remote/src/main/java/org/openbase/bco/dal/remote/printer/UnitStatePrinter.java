package org.openbase.bco.dal.remote.printer;

/*-
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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

import com.google.protobuf.Message;
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.layer.service.ServiceStateProcessor;
import org.openbase.bco.dal.lib.layer.service.ServiceStateProvider;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.remote.layer.unit.CustomUnitPool;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.remote.printer.UnitStatePrinter.Config.PrintFormat;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.extension.type.processing.MultiLanguageTextProcessor;
import org.openbase.jul.iface.Manageable;
import org.openbase.jul.pattern.Filter;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.consumer.Consumer;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.type.domotic.action.ActionDescriptionType;
import org.openbase.type.domotic.action.ActionReferenceType;
import org.openbase.type.domotic.service.ServiceDescriptionType.ServiceDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.service.ServiceTempusTypeType.ServiceTempusType.ServiceTempus;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

public class UnitStatePrinter implements Manageable<Collection<Filter<UnitConfig>>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnitStatePrinter.class);

    private final Config config;
    private final CustomUnitPool customUnitPool;
    private final Observer<ServiceStateProvider<Message>, Message> unitStateObserver;
    private final PrintStream printStream;
    private final Consumer<String> outputConsumer;
    private boolean headerPrinted = false;
    private boolean active = false;

    public static class Config {

        public enum PrintFormat {
            PROLOG_ALL_VALUES,
            PROLOG_DISCRETE_VALUES,
            HUMAN_READABLE
        }

        private PrintFormat format = PrintFormat.PROLOG_ALL_VALUES;
        private boolean skipUnknownValues = false;
        private boolean printInitialStates = false;

        public PrintFormat getFormat() {
            return format;
        }

        public Config setFormat(PrintFormat format) {
            this.format = format;
            return this;
        }

        public boolean isSkipUnknownValues() {
            return skipUnknownValues;
        }

        public Config setSkipUnknownValues(boolean skipUnknownValues) {
            this.skipUnknownValues = skipUnknownValues;
            return this;
        }

        public boolean isPrintInitialStates() {
            return printInitialStates;
        }

        public Config setPrintInitialStates(boolean printInitialStates) {
            this.printInitialStates = printInitialStates;
            return this;
        }
    }

    public UnitStatePrinter(final PrintStream printStream, final Config config) throws InstantiationException {
        try {
            this.config = config;
            this.outputConsumer = null;
            this.printStream = printStream;
            this.customUnitPool = new CustomUnitPool();
            this.unitStateObserver = (source, data) -> print((Unit) source.getServiceProvider(), source.getServiceType(), data);
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public UnitStatePrinter(final Consumer<String> outputConsumer, final Config config) throws InstantiationException {
        try {
            this.config = config;
            this.outputConsumer = outputConsumer;
            this.printStream = null;
            this.customUnitPool = new CustomUnitPool();
            this.unitStateObserver = (source, data) -> print((Unit) source.getServiceProvider(), source.getServiceType(), data);
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public void init(final Filter<UnitConfig>... filters) throws InitializationException, InterruptedException {
        init(Arrays.asList(filters));
    }

    @Override
    public void init(final Collection<Filter<UnitConfig>> filters) throws InitializationException, InterruptedException {
        try {
            customUnitPool.init(filters);
            customUnitPool.addServiceStateObserver(unitStateObserver);

            // print initial unit states
            if (config.printInitialStates) {
                for (UnitConfig unitConfig : Registries.getUnitRegistry(true).getUnitConfigs()) {
                    final UnitRemote<?> unit = Units.getUnit(unitConfig, true);

                    try {
                        for (ServiceDescription serviceDescription : unit.getAvailableServiceDescriptions()) {

                            if (serviceDescription.getPattern() != ServicePattern.PROVIDER) {
                                continue;
                            }
                            print(unit, serviceDescription.getServiceType(), Services.invokeProviderServiceMethod(serviceDescription.getServiceType(), ServiceTempus.CURRENT, unit));
                        }
                    } catch (CouldNotPerformException ex) {
                        ExceptionPrinter.printHistory("Could not print " + unit, ex, LOGGER);
                    }
                }
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    private void print(Unit<?> unit, Message data) {
        try {
            for (final ServiceType serviceType : unit.getAvailableServiceTypes()) {
                print(unit, serviceType, data);
            }
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not print " + unit, ex, LOGGER);
        }
    }


    private void print(Unit<?> unit, ServiceType serviceType, Message serviceState) {

        // print human readable format
        if (config.format == PrintFormat.HUMAN_READABLE) {
            try {
                submit(MultiLanguageTextProcessor.getBestMatch(ServiceStateProcessor.getActionDescription(serviceState)));
            } catch (NotAvailableException e) {
                // in case
                submit(unit.getLabel("?") + " is updated to " + LabelProcessor.getBestMatch(Services.generateServiceStateLabel(serviceState, serviceType), "}") + ".");
            }
            return;
        }

        // print prolog style

        // print header
        if (printStream != null && !headerPrinted) {
            headerPrinted = true;
            printStream.println("/**\n" +
                    " * Service State Transitions\n" +
                    " * --> syntax: transition(unit_id, unit_alias, unit_type, initiator[system/user], service_type, timestamp, service_value_type=service_value).\n" +
                    " */");
        }
        try {

            ActionDescriptionType.ActionDescription responsibleAction;

            try {
                responsibleAction = Services.getResponsibleAction(serviceState);
                if (responsibleAction.toString().isEmpty()) {
                    throw new NotAvailableException("ResponsibleAction");
                }
            } catch (Exception ex) {
                responsibleAction = null;
            }

            // in case the responsible action is not available, we use the system as initiator because responsible actions are not available for pure provider services and those are always system generated.
            final String initiator = responsibleAction != null ? ActionDescriptionProcessor.getInitialInitiator(responsibleAction).getInitiatorType().name().toLowerCase() : "system";

            final HashSet<String> relatedUnitIds = new HashSet<>();

            if (responsibleAction != null) {

                // compute related units to filter
                for (ActionReferenceType.ActionReference cause : responsibleAction.getActionCauseList()) {
                    relatedUnitIds.add("'" + IdResolver.getId(cause.getServiceStateDescription().getUnitId()) + "'");
                }

                for (ActionReferenceType.ActionReference impact : responsibleAction.getActionImpactList()) {
                    relatedUnitIds.add("'" + IdResolver.getId(impact.getServiceStateDescription().getUnitId()) + "'");
                }
            }

            switch (config.format) {
                case PROLOG_ALL_VALUES:
                    // print technical representation
                    for (String extractServiceState : Services.generateServiceStateStringRepresentation(serviceState, serviceType)) {
                        submit("transition("
                                + "'" + IdResolver.getId(unit) + "', "
                                + "'" + unit.getConfig().getAlias(0) + "', "
                                + unit.getUnitType().name().toLowerCase() + ", "
                                + initiator + ", "
                                + extractServiceState + ", "
                                + "[" + StringProcessor.transformCollectionToString(relatedUnitIds, ", ") + "]).");
                    }
                    break;
                case PROLOG_DISCRETE_VALUES:
                    // print technical representation
                    try {
                        // submit
                        submit("transition("
                                + "'" + IdResolver.getId(unit) + "', "
                                + "'" + unit.getConfig().getAlias(0) + "', "
                                + unit.getUnitType().name().toLowerCase() + ", "
                                + initiator + ", "
                                + Services.generateServiceValueStringRepresentation(serviceState, serviceType, config.skipUnknownValues) + ", "
                                + "[" + StringProcessor.transformCollectionToString(relatedUnitIds, ", ") + "]).");
                    } catch (NotAvailableException ex) {
                        // in case the service value is not available, its not a discrete value and its print will be skipped.
                    } catch (InvalidStateException ex) {
                        // in case the service value is unknown we just skip the print.
                    }
                    break;
                case HUMAN_READABLE:
                    // already handled above
                    break;
                default:
                    LOGGER.warn("Unknown format selected! Skip state printing...");
            }

        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not print " + serviceType.name() + " of " + unit, ex, LOGGER);
        }
    }

    private void submit(final String state) {
        if (printStream != null) {
            printStream.println(state);
        }

        if (outputConsumer != null) {
            outputConsumer.consume(state);
        }
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        customUnitPool.activate();
        active = true;
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        active = false;
        customUnitPool.deactivate();
    }

    @Override
    public boolean isActive() {
        return active;
    }
}
