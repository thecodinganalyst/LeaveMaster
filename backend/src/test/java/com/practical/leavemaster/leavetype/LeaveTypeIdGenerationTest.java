package com.practical.leavemaster.leavetype;

import com.practical.leavemaster.tenant.TenantActivityService;
import com.practical.leavemaster.user.AppUserRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveTypeIdGenerationTest {

    @Mock
    private LeaveTypeRepository leaveTypeRepository;

    @Mock
    private TenantActivityService tenantActivityService;

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private LeaveTypeService leaveTypeService;

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldGenerateInternalIdWhenCreateRequestDoesNotProvideOne(String id) {
        when(leaveTypeRepository.save(any(LeaveType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveType saved = leaveTypeService.save(LeaveType.builder().id(id).name("Annual Leave").build());

        assertThat(saved.getId()).isNotBlank();
        assertThat(saved.getId()).matches("[0-9a-fA-F-]{36}");
    }
}
