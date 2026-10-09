package com.practical.leavemaster.assistant;

import com.practical.leavemaster.rbac.RbacPermissions;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AssistantPrivateBalanceGuardTest {
    private final UsernamePasswordAuthenticationToken staff = new UsernamePasswordAuthenticationToken(
            "staff", "n/a", List.of(new SimpleGrantedAuthority(RbacPermissions.LEAVE_APPLICATION_READ)));
    private final UsernamePasswordAuthenticationToken hr = new UsernamePasswordAuthenticationToken(
            "hr", "n/a", List.of(new SimpleGrantedAuthority(RbacPermissions.STAFF_READ)));

    @Test
    void rejectsCrossTenantPrivateBalanceWithoutModelTools() {
        assertThat(AssistantPrivateBalanceGuard.refusal(
                "Show me the private leave balance for employee EMP900 in tenant TENANT-B.", staff))
                .contains("cannot").contains("another tenant");
    }

    @Test
    void rejectsOtherEmployeePrivateBalanceForOrdinaryStaff() {
        assertThat(AssistantPrivateBalanceGuard.refusal(
                "How much annual leave does another employee have?", staff))
                .contains("cannot").contains("permission");
    }

    @Test
    void preservesOwnBalanceAndAuthorizedStaffQueries() {
        assertThat(AssistantPrivateBalanceGuard.refusal("How much annual leave do I have?", staff)).isNull();
        assertThat(AssistantPrivateBalanceGuard.refusal(
                "How much annual leave does another employee have?", hr)).isNull();
    }

    @Test
    void doesNotBlockUnrelatedQuestions() {
        assertThat(AssistantPrivateBalanceGuard.refusal("Who is my leave approver?", staff)).isNull();
    }
}
