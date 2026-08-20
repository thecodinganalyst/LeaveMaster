package com.practical.leavemaster.tenant;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class TenantJurisdictionTest {

    @Test
    void shouldBuildStableAssociationIdAndCreatedTimestamp() {
        TenantJurisdiction association = TenantJurisdiction.builder()
                .id(TenantJurisdiction.idFor("ACME", "SG"))
                .tenantId("ACME")
                .jurisdictionId("SG")
                .build();

        association.initializeCreatedAt();

        assertThat(association.getId()).isEqualTo("ACME:SG");
        assertThat(association.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldApplyCalendarDefaultsWithoutChangingExplicitDates() {
        LocalDate defaultStart = LocalDate.of(2026, 1, 1);
        LocalDate defaultEnd = LocalDate.of(2026, 12, 31);
        TenantJurisdictionProvisionRequest request = new TenantJurisdictionProvisionRequest("SG", true, false, null, null);

        TenantJurisdictionProvisionRequest normalized = request.withCalendarDefaults(defaultStart, defaultEnd);

        assertThat(normalized.calendarStart()).isEqualTo(defaultStart);
        assertThat(normalized.calendarEnd()).isEqualTo(defaultEnd);
        assertThat(normalized.shouldIncludePublicHolidays()).isTrue();
        assertThat(normalized.shouldIncludeLeaveConfiguration()).isFalse();

        LocalDate explicitStart = LocalDate.of(2027, 1, 1);
        LocalDate explicitEnd = LocalDate.of(2027, 12, 31);
        TenantJurisdictionProvisionRequest explicit = new TenantJurisdictionProvisionRequest("SG", false, true, explicitStart, explicitEnd);
        assertThat(explicit.withCalendarDefaults(defaultStart, defaultEnd).calendarStart()).isEqualTo(explicitStart);
        assertThat(explicit.withCalendarDefaults(defaultStart, defaultEnd).calendarEnd()).isEqualTo(explicitEnd);
    }
}
