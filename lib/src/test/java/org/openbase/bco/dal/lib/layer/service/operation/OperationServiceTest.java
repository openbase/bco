package org.openbase.bco.dal.lib.layer.service.operation;

/*-
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
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

import org.openbase.jul.exception.VerificationFailedException;

import static org.junit.jupiter.api.Assertions.*;

class OperationServiceTest {

    @org.junit.jupiter.api.Test
    void verifyValueRange() throws VerificationFailedException {
        OperationService.verifyValueRange("good", 10, 5, 15);
        OperationService.verifyValueRange("good", 0.10, 0.05, 0.15);
        OperationService.verifyValueRange("good", 1., 1., 1.);

        try {
            OperationService.verifyValueRange("bad", 0., 1., 1.);
            fail("verification did no fail for bad value.");
        } catch (VerificationFailedException ex) {
            // this should happen
        }

        try {
            OperationService.verifyValueRange("bad", 3, 4, 9);
            fail("verification did no fail for bad value.");
        } catch (VerificationFailedException ex) {
            // this should happen
        }

        try {
            OperationService.verifyValueRange("bad", 109290, -232, 0);
            fail("verification did no fail for bad value.");
        } catch (VerificationFailedException ex) {
            // this should happen
        }
    }

    @org.junit.jupiter.api.Test
    void verifyValue() throws VerificationFailedException {
        OperationService.verifyValue("good", 10, 11, 2);
        OperationService.verifyValue("good", 0.12, 0.11, 0.1);
        OperationService.verifyValue("good", -210, 0, 240);
        OperationService.verifyValue("good", 4, 4, 0);

        try {
            OperationService.verifyValue("bad", 109290, -232, 300);
            fail("verification did no fail for bad value.");
        } catch (VerificationFailedException ex) {
            // this should happen
        }

        try {
            OperationService.verifyValue("bad", 0, 10, 4);
            fail("verification did no fail for bad value.");
        } catch (VerificationFailedException ex) {
            // this should happen
        }

        try {
            OperationService.verifyValue("bad", 0.98, 0.01, 0.96);
            fail("verification did no fail for bad value.");
        } catch (VerificationFailedException ex) {
            // this should happen
        }
    }

    @org.junit.jupiter.api.Test
    void testEquals() {
        assertTrue(OperationService.equals(30d, 30d, 0d), "equals check result invalid");
        assertTrue(OperationService.equals(30d, 33d, 4d), "equals check result invalid");
        assertTrue(OperationService.equals(1.0d, 1.0d, 0d), "equals check result invalid");
        assertTrue(OperationService.equals(0.1d, 0.2d, 0.1d), "equals check result invalid");
        assertFalse(OperationService.equals(0.1d, 0.4d, 0.1d), "equals check result invalid");
        assertFalse(OperationService.equals(30d, 31d, 0d), "equals check result invalid");
        assertFalse(OperationService.equals(-32d, 30d, 4d), "equals check result invalid");
    }
}
