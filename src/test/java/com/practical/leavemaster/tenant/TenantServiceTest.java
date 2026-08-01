package com.practical.leavemaster.tenant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private TenantService tenantService;

    @Test
    void shouldReturnAllTenants() {
        List<Tenant> tenants = List.of(
                Tenant.builder().id("t1").name("Tenant 1").startDate(LocalDate.now()).status(TenantStatus.ACTIVE).build(),
                Tenant.builder().id("t2").name("Tenant 2").startDate(LocalDate.now()).status(TenantStatus.DORMANT).build()
        );
        when(tenantRepository.findAll()).thenReturn(tenants);

        List<Tenant> result = tenantService.findAll();

        assertThat(result).hasSize(2);
    }

    @Test
    void shouldReturnTenantById() {
        Tenant tenant = Tenant.builder().id("t1").name("Tenant 1").startDate(LocalDate.now()).status(TenantStatus.ACTIVE).build();
        when(tenantRepository.findById("t1")).thenReturn(Optional.of(tenant));

        Optional<Tenant> result = tenantService.findById("t1");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Tenant 1");
    }

    @Test
    void shouldSaveTenant() {
        Tenant tenant = Tenant.builder().id("t1").name("Tenant 1").startDate(LocalDate.now()).status(TenantStatus.ACTIVE).build();
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Tenant result = tenantService.save(tenant);

        assertThat(result.getId()).isEqualTo("t1");
    }

    @Test
    void shouldUpdateTenant() {
        Tenant existing = Tenant.builder().id("t1").name("Old Name").startDate(LocalDate.of(2024, 1, 1)).status(TenantStatus.ACTIVE).build();
        Tenant updated = Tenant.builder().id("t1").name("New Name").startDate(LocalDate.of(2024, 1, 1)).endDate(LocalDate.of(2025, 12, 31)).status(TenantStatus.DORMANT).build();
        when(tenantRepository.findById("t1")).thenReturn(Optional.of(existing));
        when(tenantRepository.save(existing)).thenReturn(existing);

        Tenant result = tenantService.update("t1", updated);

        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getStatus()).isEqualTo(TenantStatus.DORMANT);
        assertThat(result.getEndDate()).isEqualTo(LocalDate.of(2025, 12, 31));
    }

    @Test
    void shouldThrowWhenUpdatingNonExistentTenant() {
        when(tenantRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.update("nonexistent", new Tenant()))
                .isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    void shouldDeleteTenant() {
        Tenant tenant = Tenant.builder().id("t1").name("Tenant 1").startDate(LocalDate.now()).status(TenantStatus.ACTIVE).build();
        when(tenantRepository.findById("t1")).thenReturn(Optional.of(tenant));

        tenantService.delete("t1");

        verify(tenantRepository).deleteById("t1");
    }

    @Test
    void shouldThrowWhenDeletingNonExistentTenant() {
        when(tenantRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.delete("nonexistent"))
                .isInstanceOf(TenantNotFoundException.class);

        verify(tenantRepository, never()).deleteById(any());
    }
}
