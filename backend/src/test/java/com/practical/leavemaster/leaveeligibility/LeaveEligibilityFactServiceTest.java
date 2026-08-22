package com.practical.leavemaster.leaveeligibility;

import com.practical.leavemaster.rbac.AppRole;
import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffRepository;
import com.practical.leavemaster.tenant.TenantActivityService;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveEligibilityFactServiceTest {

    @Mock StaffDependantRepository dependantRepository;
    @Mock QualifyingLeaveEventRepository eventRepository;
    @Mock StaffRepository staffRepository;
    @Mock AppUserRepository appUserRepository;
    @Mock TenantActivityService tenantActivityService;

    LeaveEligibilityFactService service;
    Staff staff;

    @BeforeEach
    void setUp() {
        service = new LeaveEligibilityFactService(dependantRepository, eventRepository, staffRepository,
                appUserRepository, tenantActivityService);
        staff = Staff.builder().id("S1").name("Staff One").joinDate(LocalDate.of(2020, 1, 1)).tenantId("T1").build();
        when(staffRepository.findById("S1")).thenReturn(Optional.of(staff));
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsAndUpdatesDependantWithNormalizedReusableFacts() {
        StaffDependantWriteRequest create = dependantRequest(null);
        when(dependantRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        StaffDependant saved = service.createDependant("S1", create);
        assertNotNull(saved.getId());
        assertEquals("T1", saved.getTenantId());
        assertEquals("S1", saved.getStaffId());
        assertEquals("CHILD", saved.getRelationshipCode());
        assertEquals("SG", saved.getCitizenshipCode());
        assertTrue(saved.isActive());
        verify(tenantActivityService).touch("T1");

        StaffDependant existing = saved;
        when(dependantRepository.findById(saved.getId())).thenReturn(Optional.of(existing));
        StaffDependantWriteRequest update = new StaffDependantWriteRequest("Updated", "parent", null,
                "my", null, null, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), false);

        StaffDependant updated = service.updateDependant("S1", saved.getId(), update);
        assertEquals("Updated", updated.getName());
        assertEquals("PARENT", updated.getRelationshipCode());
        assertEquals("MY", updated.getCitizenshipCode());
        assertFalse(updated.isActive());
    }

    @Test
    void validatesDependantDatesAndRequiredFields() {
        assertThrows(IllegalArgumentException.class, () -> service.createDependant("S1", null));
        assertThrows(IllegalArgumentException.class, () -> service.createDependant("S1",
                new StaffDependantWriteRequest(" ", "child", null, null, null, null, null, null, true)));
        assertThrows(IllegalArgumentException.class, () -> service.createDependant("S1",
                new StaffDependantWriteRequest("A", " ", null, null, null, null, null, null, true)));
        assertThrows(IllegalArgumentException.class, () -> service.createDependant("S1",
                new StaffDependantWriteRequest("A", "child", null, null, null, null,
                        LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1), true)));
        assertThrows(IllegalArgumentException.class, () -> service.createDependant("S1",
                new StaffDependantWriteRequest("A", "child", LocalDate.of(2020, 1, 1), null, null,
                        LocalDate.of(2019, 1, 1), null, null, true)));
    }

    @Test
    void listsReadsAndDeletesOnlyOwnedDependants() {
        StaffDependant dependant = StaffDependant.builder().id("D1").tenantId("T1").staffId("S1").name("D")
                .relationshipCode("CHILD").active(true).build();
        when(dependantRepository.findAllByStaffId("S1")).thenReturn(List.of(dependant));
        when(dependantRepository.findById("D1")).thenReturn(Optional.of(dependant));
        when(eventRepository.existsByDependantId("D1")).thenReturn(false);

        assertEquals(List.of(dependant), service.findDependants("S1"));
        assertSame(dependant, service.findDependant("S1", "D1"));
        service.deleteDependant("S1", "D1");
        verify(dependantRepository).delete(dependant);

        when(eventRepository.existsByDependantId("D1")).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> service.deleteDependant("S1", "D1"));

        StaffDependant other = StaffDependant.builder().id("D2").tenantId("T2").staffId("S2").build();
        when(dependantRepository.findById("D2")).thenReturn(Optional.of(other));
        assertThrows(NoSuchElementException.class, () -> service.findDependant("S1", "D2"));
    }

    @Test
    void createsUpdatesListsAndDeletesQualifyingEvents() {
        StaffDependant dependant = StaffDependant.builder().id("D1").tenantId("T1").staffId("S1").build();
        when(dependantRepository.findById("D1")).thenReturn(Optional.of(dependant));
        when(eventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        QualifyingLeaveEventWriteRequest create = eventRequest("D1", null);
        QualifyingLeaveEvent event = service.createEvent("S1", create);
        assertNotNull(event.getId());
        assertEquals("BIRTH", event.getEventTypeCode());
        assertEquals(QualifyingEventStatus.RECORDED, event.getStatus());
        assertEquals("ref", event.getExternalReference());
        assertNull(event.getSupportingDocumentReference());

        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
        QualifyingLeaveEventWriteRequest update = new QualifyingLeaveEventWriteRequest(null, "adoption",
                LocalDate.of(2026, 3, 1), null, null, null, "doc-1", QualifyingEventStatus.VERIFIED);
        QualifyingLeaveEvent updated = service.updateEvent("S1", event.getId(), update);
        assertNull(updated.getDependantId());
        assertEquals("ADOPTION", updated.getEventTypeCode());
        assertEquals(QualifyingEventStatus.VERIFIED, updated.getStatus());
        assertEquals("doc-1", updated.getSupportingDocumentReference());

        when(eventRepository.findAllByStaffId("S1")).thenReturn(List.of(updated));
        assertEquals(List.of(updated), service.findEvents("S1"));
        assertSame(updated, service.findEvent("S1", event.getId()));
        service.deleteEvent("S1", event.getId());
        verify(eventRepository).delete(updated);
    }

    @Test
    void validatesEventFactsAndDependantOwnership() {
        assertThrows(IllegalArgumentException.class, () -> service.createEvent("S1", null));
        assertThrows(IllegalArgumentException.class, () -> service.createEvent("S1",
                new QualifyingLeaveEventWriteRequest(null, " ", LocalDate.now(), null, null, null, null, null)));
        assertThrows(IllegalArgumentException.class, () -> service.createEvent("S1",
                new QualifyingLeaveEventWriteRequest(null, "BIRTH", null, null, null, null, null, null)));
        assertThrows(IllegalArgumentException.class, () -> service.createEvent("S1",
                new QualifyingLeaveEventWriteRequest(null, "BIRTH", LocalDate.now(), LocalDate.of(2026, 2, 1),
                        LocalDate.of(2026, 1, 1), null, null, null)));

        when(dependantRepository.findById("missing")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.createEvent("S1", eventRequest("missing", null)));

        StaffDependant other = StaffDependant.builder().id("D2").tenantId("T2").staffId("S2").build();
        when(dependantRepository.findById("D2")).thenReturn(Optional.of(other));
        assertThrows(NoSuchElementException.class, () -> service.createEvent("S1", eventRequest("D2", null)));
    }

    @Test
    void tenantUserIsScopedAndPlatformAdminCannotMutate() {
        authenticate(tenantUser("tenant-user", "T1"));
        StaffDependant dependant = StaffDependant.builder().id("D1").tenantId("T1").staffId("S1").build();
        when(dependantRepository.findAllByTenantIdAndStaffId("T1", "S1")).thenReturn(List.of(dependant));
        when(eventRepository.findAllByTenantIdAndStaffId("T1", "S1")).thenReturn(List.of());
        assertEquals(1, service.findDependants("S1").size());
        assertTrue(service.findEvents("S1").isEmpty());

        SecurityContextHolder.clearContext();
        authenticate(tenantUser("other-user", "T2"));
        assertThrows(NoSuchElementException.class, () -> service.findDependants("S1"));
        assertThrows(AccessDeniedException.class, () -> service.createDependant("S1", dependantRequest(true)));

        SecurityContextHolder.clearContext();
        authenticate(platformAdmin());
        assertThrows(AccessDeniedException.class, () -> service.createDependant("S1", dependantRequest(true)));
        assertThrows(AccessDeniedException.class, () -> service.createEvent("S1", eventRequest(null, null)));
    }

    @Test
    void missingStaffAndFactsReturnNotFoundSemantics() {
        when(staffRepository.findById("missing")).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> service.findDependants("missing"));
        assertThrows(NoSuchElementException.class, () -> service.createDependant("missing", dependantRequest(true)));

        when(dependantRepository.findById("missingD")).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> service.findDependant("S1", "missingD"));
        when(eventRepository.findById("missingE")).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> service.findEvent("S1", "missingE"));
    }

    private StaffDependantWriteRequest dependantRequest(Boolean active) {
        return new StaffDependantWriteRequest(" Child ", "child", LocalDate.of(2020, 1, 1),
                "sg", " ", null, null, null, active);
    }

    private QualifyingLeaveEventWriteRequest eventRequest(String dependantId, QualifyingEventStatus status) {
        return new QualifyingLeaveEventWriteRequest(dependantId, " birth ", LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 10), " ref ", " ", status);
    }

    private AppUser tenantUser(String login, String tenantId) {
        return AppUser.builder().loginName(login).password("x").active(true).tenantId(tenantId).build();
    }

    private AppUser platformAdmin() {
        AppRole role = AppRole.builder().id("PLATFORM_ADMIN").description("Platform admin").active(true).build();
        return AppUser.builder().loginName("platform").password("x").active(true).roles(Set.of(role)).build();
    }

    private void authenticate(AppUser user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getLoginName(), "n/a", List.of()));
        when(appUserRepository.findById(user.getLoginName())).thenReturn(Optional.of(user));
    }
}
