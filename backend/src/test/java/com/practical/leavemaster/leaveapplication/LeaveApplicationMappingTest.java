package com.practical.leavemaster.leaveapplication;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class LeaveApplicationMappingTest {

    @Test
    void shouldHaveAttachmentUrlFieldInsteadOfBinaryAttachment() throws NoSuchFieldException {
        Field attachmentUrl = LeaveApplication.class.getDeclaredField("attachmentUrl");

        assertThat(attachmentUrl.getType()).isEqualTo(String.class);
        assertThat(attachmentUrl.getAnnotation(jakarta.persistence.Lob.class)).isNull();
    }

    @Test
    void shouldNotHaveBinaryAttachmentField() {
        assertThat(java.util.Arrays.stream(LeaveApplication.class.getDeclaredFields())
                .map(Field::getName)
                .noneMatch(name -> name.equals("attachment"))).isTrue();
    }
}
