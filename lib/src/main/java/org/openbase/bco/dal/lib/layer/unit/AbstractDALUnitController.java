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
import com.google.protobuf.GeneratedMessage;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.openbase.bco.dal.lib.layer.service.ServiceFactory;
import org.openbase.bco.dal.lib.layer.service.ServiceFactoryProvider;
import org.openbase.bco.dal.lib.layer.service.provider.ProviderService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.MultiException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import rst.domotic.service.ServiceDescriptionType.ServiceDescription;
import rst.domotic.service.ServiceTemplateType;
import rst.domotic.unit.UnitConfigType;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 * @param <M> Underling message type.
 * @param <MB> Message related builder.
 */
public abstract class AbstractDALUnitController<M extends GeneratedMessage, MB extends M.Builder<MB>> extends AbstractUnitController<M, MB> implements ServiceFactoryProvider {

    private final UnitHost unitHost;
    private final ServiceFactory serviceFactory;

    public AbstractDALUnitController(final Class unitClass, final UnitHost unitHost, final MB builder) throws InstantiationException {
        super(unitClass, builder);
        try {
            if (unitHost.getServiceFactory() == null) {
                throw new NotAvailableException("service factory");
            }
            this.unitHost = unitHost;
            this.serviceFactory = unitHost.getServiceFactory();
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    public ServiceFactory getServiceFactory() throws NotAvailableException {
        return serviceFactory;
    }

    public UnitHost getUnitHost() {
        return unitHost;
    }

    @Override
    public void init(final UnitConfigType.UnitConfig config) throws InitializationException, InterruptedException {
        super.init(config);

        try {
            verifyUnitConfig();
        } catch (VerificationFailedException ex) {
            ExceptionPrinter.printHistory(new InvalidStateException(this + " is not valid!", ex), logger);
        }
    }

    /**
     * Verify if all provider service update methods are registered for given
     * configuration.
     *
     * @throws VerificationFailedException is thrown if the check fails or at
     * least on update method is not available.
     */
    private void verifyUnitConfig() throws VerificationFailedException {
        try {
            logger.debug("Validating unit update methods...");

            MultiException.ExceptionStack exceptionStack = null;
            List<String> unitMethods = new ArrayList<>();
            String updateMethod;

            // === Load unit methods. ===
            for (Method medhod : getClass().getMethods()) {
                unitMethods.add(medhod.getName());
            }

            // === Verify if all update methods are registered. ===
            for (ServiceDescription serviceDescription : getUnitTemplate().getServiceDescriptionList()) {

                // filter other services than provider
                if (serviceDescription.getPattern() != ServiceTemplateType.ServiceTemplate.ServicePattern.PROVIDER) {
                    continue;
                }

                // verify
                updateMethod = ProviderService.getUpdateMethodName(serviceDescription.getType());
                if (!unitMethods.contains(updateMethod)) {
                    exceptionStack = MultiException.push(serviceDescription, new NotAvailableException("Method", updateMethod), exceptionStack);
                }
            }

            // === throw multi exception in error case. ===
            MultiException.checkAndThrow("At least one update method missing!", exceptionStack);
        } catch (CouldNotPerformException ex) {
            throw new VerificationFailedException("UnitTemplate is not compatible for configured unit controller!", ex);
        }
    }
}
