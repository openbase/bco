package org.openbase.bco.app.cloudconnector.jp;

/*-
 * #%L
 * BCO Cloud Connector
 * %%
 * Copyright (C) 2018 - 2020 openbase.org
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

import java.net.URI;
import java.util.List;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class JPCloudServerURI extends AbstractJavaProperty<URI> {

    private static final String[] ARGUMENT_IDENTIFIERS = {"URI"};
    private static final String[] COMMAND_IDENTIFIERS = {"--cloud"};

    private static final String DEFAULT_URI = "https://bco-cloud.herokuapp.com/";

    public JPCloudServerURI() {
        super(COMMAND_IDENTIFIERS);
    }

    @Override
    protected String[] generateArgumentIdentifiers() {
        return ARGUMENT_IDENTIFIERS;
    }

    @Override
    protected URI getPropertyDefaultValue() {
        return URI.create(DEFAULT_URI);
    }

    @Override
    protected URI parse(List<String> list) throws Exception {
        String uri = getOneArgumentResult();
        if (!uri.startsWith("http")) {
            uri = "http://" + uri;
        }
        return new URI(uri);
    }

    @Override
    public String getDescription() {
        return "Define the URI of the BCO Cloud to which the connector should connect.";
    }
}
