package org.openbase.bco.registry.lib.jp;

/*-
 * #%L
 * BCO Registry Lib
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
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

import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jps.tools.FileHandler.AutoMode;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.storage.registry.jp.AbstractJPGitDatabaseDirectory;

import java.io.File;

public class AbstractJPGitBCODatabaseDirectory extends AbstractJPGitDatabaseDirectory {


    public final String dbIdentifier;

    public AbstractJPGitBCODatabaseDirectory(final Class clazz) {
        super(detectCommandIdentifier(clazz));
        dbIdentifier = detectDBIdentifier(clazz);
        setAutoCreateMode(AutoMode.On);
        registerDependingProperty(JPBCODatabaseDirectory.class);
    }

    private static String[] detectCommandIdentifier(final Class clazz) {
        final String[] id = {"--"+detectDBIdentifier(clazz)};
        return id;
    }

    private static String detectDBIdentifier(final Class clazz) {
        return StringProcessor.transformToKebabCase(clazz.getSimpleName().replace("JP", "").replace("DatabaseDirectory", "-db"));
    }

    @Override
    protected String getRepositoryURL() {
        return "https://github.com/openbase/bco.registry."+dbIdentifier+".git";
    }

    @Override
    public File getParentDirectory() throws JPNotAvailableException {
        return JPService.getProperty(JPBCODatabaseDirectory.class).getValue();
    }

    @Override
    protected File getPropertyDefaultValue() {
        return new File(dbIdentifier);
    }
}
