package org.openbase.bco.dal.lib.layer.unit;

/*-
 * #%L
 * BCO DAL Library
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

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import org.openbase.bco.dal.lib.layer.service.OperationServiceFactoryProvider;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

public interface HostUnitController<D extends AbstractMessage, DB extends D.Builder<DB>> extends BaseUnitController<D, DB>, HostUnit<D>, OperationServiceFactoryProvider {

    UnitController<?, ?> getHostedUnitController(String id) throws NotAvailableException;

    List<UnitController<?, ?>> getHostedUnitControllerList();

    List<UnitConfig> getHostedUnitConfigList() throws NotAvailableException, InterruptedException;
}
