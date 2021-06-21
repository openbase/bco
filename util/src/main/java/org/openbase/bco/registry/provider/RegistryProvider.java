package org.openbase.bco.registry.provider;

/*
 * #%L
 * BCO Registry Utility
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import org.openbase.bco.registry.activity.lib.provider.ActivityRegistryProvider;
import org.openbase.bco.registry.clazz.lib.provider.ClassRegistryProvider;
import org.openbase.bco.registry.template.lib.provider.TemplateRegistryProvider;
import org.openbase.bco.registry.unit.lib.provider.UnitRegistryProvider;

/**
 * Interface provides a collection of globally managed registry instances.
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface RegistryProvider extends UnitRegistryProvider, ActivityRegistryProvider, TemplateRegistryProvider, ClassRegistryProvider {

}
