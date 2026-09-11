package com.practical.leavemaster.testsupport;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScenarioDataFactoryTest {

    private static final LocalDate REFERENCE_DATE = LocalDate.of(2026, 9, 12);

    @Test
    void createsStandardSingaporeScenarioWithRepresentativeStaffAndRoles() {
        var scenario = ScenarioDataFactory.standardSingaporeScenario("worker-1", REFERENCE_DATE);

        assertEquals("E2E-worker-1", scenario.tenant().getId());
        assertEquals("SG", scenario.tenant().getJurisdictionId());
        assertEquals(9, scenario.staff().size());
        assertEquals(9, scenario.users().size());
        assertEquals(4, scenario.roles().size());
        assertEquals(5, scenario.entitlements().size());
        assertEquals(4, scenario.approvers().size());
        assertEquals(1, scenario.dependants().size());
    }

    @Test
    void createsApproverRelationshipsAndLeavesMissingApproverCaseExplicit() {
        var scenario = ScenarioDataFactory.standardSingaporeScenario("approvers", REFERENCE_DATE);

        assertTrue(scenario.approvers().stream().anyMatch(a ->
                a.getStaff().equals(scenario.staff("staff001"))
                        && a.getApprover().equals(scenario.staff("manager01"))));
        assertFalse(scenario.approvers().stream().anyMatch(a ->
                a.getStaff().equals(scenario.staff("staff005"))));
    }

    @Test
    void generatesEntitlementsUsingEmploymentDates() {
        var scenario = ScenarioDataFactory.standardSingaporeScenario("entitlements", REFERENCE_DATE);

        var normal = scenario.entitlements().get("staff001");
        var midYear = scenario.entitlements().get("staff002");

        assertEquals(new BigDecimal("14.00"), normal.getEntitlement());
        assertEquals(LocalDate.of(2026, 1, 1), normal.getFrom());
        assertEquals(new BigDecimal("7.00"), midYear.getEntitlement());
        assertEquals(LocalDate.of(2026, 7, 1), midYear.getFrom());
        assertEquals(midYear, scenario.staff("staff002").getLeaveEntitlements().getFirst());
    }

    @Test
    void supportsJurisdictionOverrideWithoutMutatingOtherScenarioStaff() {
        var scenario = ScenarioDataFactory.standardSingaporeScenario("jurisdiction", REFERENCE_DATE);
        var original = scenario.staff("staff004");
        var overridden = ScenarioDataFactory.withJurisdiction(original, "AU-NSW");

        assertEquals("SG", original.getJurisdictionId());
        assertEquals("AU-NSW", overridden.getJurisdictionId());
        assertEquals(original.getId(), overridden.getId());
    }

    @Test
    void scenarioIdentifiersPreventCollisionsAcrossRuns() {
        var first = ScenarioDataFactory.standardSingaporeScenario("worker-1", REFERENCE_DATE);
        var second = ScenarioDataFactory.standardSingaporeScenario("worker-2", REFERENCE_DATE);

        assertNotEquals(first.tenant().getId(), second.tenant().getId());
        assertNotEquals(first.staff("staff001").getId(), second.staff("staff001").getId());
        assertNotEquals(first.user("manager01").getUserId(), second.user("manager01").getUserId());
    }

    @Test
    void exposesDependantsForEligibilityScenarios() {
        var scenario = ScenarioDataFactory.standardSingaporeScenario("dependants", REFERENCE_DATE);
        var dependant = scenario.dependants().getFirst();

        assertEquals(scenario.staff("staff001").getId(), dependant.getStaffId());
        assertEquals("CHILD", dependant.getRelationshipCode());
        assertTrue(dependant.isActive());
        assertNotNull(scenario.staff("staff001").getPreviewDependants());
    }

    @Test
    void rejectsBlankScenarioIdentifier() {
        assertThrows(IllegalArgumentException.class,
                () -> ScenarioDataFactory.standardSingaporeScenario(" ", REFERENCE_DATE));
    }
}
