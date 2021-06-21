package org.openbase.bco.authentication.lib;

/*-
 * #%L
 * BCO Authentication Library
 * %%
 * Copyright (C) 2017 - 2021 openbase.org
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

import java.util.HashMap;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.pattern.ObservableImpl;
import org.slf4j.LoggerFactory;

/**
 * This class can be used to keep track of frequently occurring exceptions.
 * Exceptions are grouped by their type (class) and by the place they occurred
 * (class, method, line number, accessed over the stack trace).
 * Observers can subscribe to be notified when an exception is reported frequently from the same place.
 *
 * @author <a href="mailto:cromankiewicz@techfak.uni-bielefeld.de">Constantin Romankiewicz</a>
 */
public class ExceptionReporter extends ObservableImpl<ExceptionReporter, ExceptionReporter.ExceptionReport> {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ExceptionReporter.class);

    private static ExceptionReporter instance;

    private HashMap<Class<? extends Throwable>, HashMap<StackTraceElement, ExceptionReport>> reports;

    public static ExceptionReporter getInstance() {
        if (instance == null) {
            instance = new ExceptionReporter();
        }

        return instance;
    }

    private ExceptionReporter() {
        reports = new HashMap<>();
    }

    /**
     * Reports an exception.
     * Without a limit given, the default limit is set to -1, meaning that no observers will be
     * notified at any point, except if the limit has already been set before.
     * @param th The exception to report.
     */
    public void report(Throwable th) {
        report(th, -1);
    }

    /**
     * Reports an exception.
     * If more exceptions are reported from the same place than the limit allows, all observers of this instance will be notified.
     * @param th The exception to report.
     * @param limit How many times this exception may occur from the same place, until observers are notified.
     */
    public void report(Throwable th, int limit) {
        assert th.getStackTrace().length > 0;

        StackTraceElement source = th.getStackTrace()[0];

        assert source != null;

        // Get all previous reports for this type of exception.
        HashMap<StackTraceElement, ExceptionReport> reportCollection = reports.get(th.getClass());

        if (reportCollection == null) {
            reportCollection = new HashMap<>();
            reports.put(th.getClass(), reportCollection);
        }

        // Get the previous report (if any) for the same type of exception, occurring at the same place.
        ExceptionReport report = reportCollection.get(source);

        if (report != null) {
            report.addCount();
        }
        else {
            report = new ExceptionReport(th, 1, limit);
            reportCollection.put(source, report);
        }

        // If the report count is over the defined limit, notify observers about this frequent occurrence.
        if (report.isOverLimit()) {
            try {
                notifyObservers(report);
                report.resetCount();
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory(ex, LOGGER);
            }
        }
    }

    /**
     * Inner class holding information about how often an exception was reported.
     */
    public class ExceptionReport {
        /**
         * The exception.
         */
        private Throwable throwable;
        /**
         * How often the exception has been reported.
         */
        private int count;
        /**
         * How many times this exception may occur from the same place, until observers are notified.
         * A negative limit means observers are never notified. A zero-limit means observers are notified every time the exception occurs.
         */
        private int limit;

        public ExceptionReport(Throwable throwable) {
            this(throwable, 1, -1);
        }

        public ExceptionReport(Throwable throwable, int count) {
            this(throwable, count, -1);
        }

        public ExceptionReport(Throwable throwable, int count, int limit) {
            this.throwable = throwable;
            this.count = count;
            this.limit = limit;
        }

        public void resetCount() {
            this.count = 0;
        }

        public void addCount() {
            this.count++;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }

        private boolean isOverLimit() {
            return limit >= 0 && count > limit;
        }

        public Throwable getThrowable() {
            return throwable;
        }

        public int getCount() {
            return count;
        }

        public int getLimit() {
            return limit;
        }
    }

}
