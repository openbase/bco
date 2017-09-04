package org.openbase.bco.registry.lib.com;

/*-
 * #%L
 * BCO Registry Lib
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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

import com.google.protobuf.GeneratedMessage;

/**
 * Default filter which does not filter at all.
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 * @param <M> The type of message which is filtered.
 */
public class DefaultMessageFilter<M extends GeneratedMessage> extends AbstractFilter<M>{

    /**
     * Has nothing to do before filtering.
     */
    @Override
    public void beforeFilter() {
        // do nothing
    }

    /**
     * Accept all message.
     * 
     * @param type The message which is verified.
     * @return True for all messages.
     */
    @Override
    public boolean verify(M type) {
        // every msg is fine
        return true;
    }

}
