package org.openbase.bco.dal.lib.layer.service.operation;

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