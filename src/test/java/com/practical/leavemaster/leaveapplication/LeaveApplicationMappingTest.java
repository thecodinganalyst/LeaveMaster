package com.practical.leavemaster.leaveapplication;

import jakarta.persistence.Lob;
import org.junit.jupiter.api.Test;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class LeaveApplicationMappingTest {

    @Test
    void shouldMapAttachmentWithoutLobStorage() throws NoSuchFieldException {
        Field attachment = LeaveApplication.class.getDeclaredField("attachment");

        assertThat(attachment.getAnnotation(Lob.class)).isNull();
        assertThat(attachment.getAnnotation(JdbcTypeCode.class)).isNotNull();
        assertThat(attachment.getAnnotation(JdbcTypeCode.class).value()).isEqualTo(SqlTypes.LONGVARBINARY);
    }
}
