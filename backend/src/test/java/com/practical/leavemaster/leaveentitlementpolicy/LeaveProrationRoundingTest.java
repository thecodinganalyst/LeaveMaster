package com.practical.leavemaster.leaveentitlementpolicy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class LeaveProrationRoundingTest {

    @ParameterizedTest
    @CsvSource({
            "5.24,5.00",
            "5.25,5.50",
            "5.49,5.50",
            "5.74,5.50",
            "5.75,6.00",
            "5.79,6.00"
    })
    void shouldRoundToNearestHalfDay(String rawAmount, String expectedAmount) {
        assertThat(LeaveProrationRounding.toNearestHalfDay(new BigDecimal(rawAmount)))
                .isEqualByComparingTo(expectedAmount);
    }
}
