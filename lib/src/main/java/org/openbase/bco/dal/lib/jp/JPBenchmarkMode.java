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
import org.openbase.jps.core.AbstractJavaProperty;
import org.openbase.jps.exception.JPValidationException;
import org.openbase.jps.preset.AbstractJPBoolean;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class JPBenchmarkMode extends AbstractJPBoolean {

    public final static String[] COMMAND_IDENTIFIERS = {"--benchmark"};

    public JPBenchmarkMode() {
        super(COMMAND_IDENTIFIERS);
    }

    @Override
    public void validate() throws JPValidationException {
        super.validate();
        if (!getValueType().equals((AbstractJavaProperty.ValueType.PropertyDefault))) {
            logger.warn("Started in benchmark mode!! Make sure no hardware is connected to avoid hardware damage.");
            String userConfirmation = System.console().readLine("Please confirm the benchmark by pressing 'Y'").toLowerCase();
            if (!userConfirmation.contains("y") && !userConfirmation.contains("j") && !userConfirmation.contains("z")) {
                logger.warn("Banchmark canceled by user...");
                System.exit(22);
            }
        }
    }

    @Override
    public String getDescription() {
        return "Starts a benchmark test where high frequently changing unit states are genrated to test the global system performance.";
    }
}
