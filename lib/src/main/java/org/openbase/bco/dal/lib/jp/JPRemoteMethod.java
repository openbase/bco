package org.openbase.bco.dal.lib.jp;

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

import java.lang.reflect.Method;
import org.openbase.jps.preset.AbstractJPMethod;
import org.openbase.jul.extension.rsb.com.RSBRemoteService;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class JPRemoteMethod extends AbstractJPMethod<RSBRemoteService> {

    public final static String[] COMMAND_IDENTIFIERS = {"-m", "--remoteMethod"};

    public JPRemoteMethod() {
        super(COMMAND_IDENTIFIERS, JPRemoteService.class);
    }

    @Override
    public String getDescription() {
        return "Specifies the remote sevice methode to call.";
    }
    
    @Override
    protected Method getPropertyDefaultValue() {
        return null;
    }
}
