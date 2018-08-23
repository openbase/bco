package org.openbase.bco.app.openhab.sitemap.element;

/*-
 * #%L
 * BCO Openhab App
 * %%
 * Copyright (C) 2018 openbase.org
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

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.processing.StringProcessor;
import org.slf4j.LoggerFactory;

public abstract class AbstractSitemapElement {

    public static final int TAB_SIZE = 4;
    protected static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AbstractSitemapElement.class);
    protected final String prefix;
    private final int tabLevel;

    public AbstractSitemapElement() {
        this.tabLevel = 0;
        this.prefix = "";
    }

    public AbstractSitemapElement(final AbstractSitemapElement parentElement) {
        this.tabLevel = parentElement.getTabLevel() + 1;
        this.prefix = StringProcessor.fillWithSpaces("", tabLevel * TAB_SIZE);
    }

    public int getTabLevel() {
        return tabLevel;
    }


    public String tab() {
        return StringProcessor.fillWithSpaces("",  TAB_SIZE);
    }
    protected abstract String serialize(final String sitemap) throws CouldNotPerformException;


    public String getElement() {
        return appendElement("");
    }

    public String appendElement(final String sitemap) {
        try {
            return serialize(sitemap);
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not serialize sitemap Element[" + this + "]", ex, LOGGER);
        }
        return "";
    }
}
