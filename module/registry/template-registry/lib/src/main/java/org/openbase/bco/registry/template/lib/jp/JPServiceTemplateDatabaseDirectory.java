package org.openbase.bco.registry.template.lib.jp;

/*-
 * #%L
 * BCO Registry Template Library
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

import org.openbase.bco.registry.lib.jp.AbstractJPGitBCODatabaseDirectory;
import org.openbase.bco.registry.lib.jp.JPBCODatabaseDirectory;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.storage.registry.jp.AbstractJPDatabaseDirectory;

import java.io.File;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class JPServiceTemplateDatabaseDirectory extends AbstractJPGitBCODatabaseDirectory {

    public JPServiceTemplateDatabaseDirectory() {
        super(JPServiceTemplateDatabaseDirectory.class);
    }
}

