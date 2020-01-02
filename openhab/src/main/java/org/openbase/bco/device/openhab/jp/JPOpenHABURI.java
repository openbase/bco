package org.openbase.bco.device.openhab.jp;

/*-
 * #%L
 * BCO Openhab Device Manager
 * %%
 * Copyright (C) 2015 - 2020 openbase.org
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

import org.openbase.jps.core.AbstractJavaProperty;

import java.io.File;
import java.net.URI;
import java.util.List;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class JPOpenHABURI extends AbstractJavaProperty<URI> {

    private static final String[] ARGUMENT_IDENTIFIERS = {"URI"};
    private static final String[] COMMAND_IDENTIFIERS = {"--uri"};

    private static final String DEFAULT_URI = "http://localhost:8080";

    public static final String SYSTEM_VARIABLE_OPENHAB_PORT = "OPENHAB_HTTP_PORT";

    public JPOpenHABURI() {
        super(COMMAND_IDENTIFIERS);
    }

    @Override
    protected String[] generateArgumentIdentifiers() {
        return ARGUMENT_IDENTIFIERS;
    }

    @Override
    protected URI getPropertyDefaultValue() {
        // URI.create does not throw an exception which is fine for the default value

        // use system variable if defined
        String systemDefinedPort = System.getenv(SYSTEM_VARIABLE_OPENHAB_PORT);
        if (systemDefinedPort != null) {
            return URI.create("http://localhost:"+systemDefinedPort);
        }

        return URI.create(DEFAULT_URI);
    }

    @Override
    protected URI parse(final List<String> list) throws Exception {
        String uri = getOneArgumentResult();
        // make sure that the uri always starts with http ot https
        if (!uri.startsWith("http")) {
            uri = "http://" + uri;
        }
        // create a new uri, this will throw an exception if the uri is not valid
        return new URI(uri);
    }

    @Override
    public String getDescription() {
        return "Define the URI of the OpenHAB 2 instance this app should connect to.";
    }
}
