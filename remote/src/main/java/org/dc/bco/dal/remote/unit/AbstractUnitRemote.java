package org.dc.bco.dal.remote.unit;

/*
 * #%L
 * DAL Remote
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
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
import java.util.List;
import org.dc.bco.registry.device.remote.CachedDeviceRegistryRemote;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InvalidStateException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.rsb.com.AbstractConfigurableRemote;
import org.dc.jul.extension.rsb.scope.ScopeGenerator;
import org.dc.jul.extension.rsb.scope.ScopeTransformer;
import org.dc.jul.extension.rst.iface.ScopeProvider;
import rsb.Scope;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitTemplateType;
import rst.rsb.ScopeType;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 * @param <M>
 */
public abstract class AbstractUnitRemote<M extends GeneratedMessage> extends AbstractConfigurableRemote<M, UnitConfig> implements UnitRemote<M, UnitConfig> {

    private UnitTemplateType.UnitTemplate unitTemplate;

    @Override
    public void initById(final String id) throws InitializationException, InterruptedException {
        try {
            init(CachedDeviceRegistryRemote.getDeviceRegistry().getUnitConfigById(id));
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void initByLabel(final String label) throws InitializationException, InterruptedException {
        try {
            List<UnitConfig> unitConfigList = CachedDeviceRegistryRemote.getDeviceRegistry().getUnitConfigsByLabel(label);

            if (unitConfigList.isEmpty()) {
                throw new NotAvailableException("Unit with Label[" + label + "]");
            }

            if (unitConfigList.size() > 1) {
                throw new InvalidStateException("Unit with Label[" + label + "] is not unique!");
            }

            init(unitConfigList.get(0));
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void init(ScopeType.Scope scope) throws InitializationException, InterruptedException {
        try {
            init(CachedDeviceRegistryRemote.getDeviceRegistry().getUnitConfigByScope(scope));
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void init(Scope scope) throws InitializationException, InterruptedException {
        try {
            this.init(ScopeTransformer.transform(scope));
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void init(String scope) throws InitializationException, InterruptedException {
        try {
            this.init(ScopeGenerator.generateScope(scope));
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    public void init(final String label, final ScopeProvider location) throws InitializationException, InterruptedException {
        try {
            init(ScopeGenerator.generateScope(label, detectMessageClass().getSimpleName(), location.getScope()));
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public UnitConfig updateConfig(final UnitConfig config) throws CouldNotPerformException, InterruptedException {
        unitTemplate = CachedDeviceRegistryRemote.getDeviceRegistry().getUnitTemplateById(config.getUnitTemplateConfigId());
        return super.updateConfig(config);
    }

    @Override
    public UnitTemplateType.UnitTemplate.UnitType getType() throws NotAvailableException {
        try {
            return getConfig().getType();
        } catch (NullPointerException | NotAvailableException ex) {
            throw new NotAvailableException("unit type", ex);
        }
    }

    @Override
    public UnitTemplateType.UnitTemplate getTemplate() throws NotAvailableException {
        if (unitTemplate == null) {
            throw new NotAvailableException("unit template");
        }
        return unitTemplate;
    }

    @Override
    public String getLabel() throws NotAvailableException {
        try {
            return getConfig().getLabel();
        } catch (NullPointerException | NotAvailableException ex) {
            throw new NotAvailableException("unit label", ex);
        }
    }

    @Override
    public ScopeType.Scope getScope() throws NotAvailableException {
        try {
            return getConfig().getScope();
        } catch (NullPointerException | CouldNotPerformException ex) {
            throw new NotAvailableException("unit label", ex);
        }
    }
}
