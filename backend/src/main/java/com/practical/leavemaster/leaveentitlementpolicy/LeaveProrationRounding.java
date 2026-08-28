package com.practical.leavemaster.leaveentitlementpolicy;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Shared rounding rules for prorated leave entitlements.
 */
public final class LeaveProrationRounding {
    public static final BigDecimal HALF_DAY = new BigDecimal("0.50");

    private LeaveProrationRounding() {
    }

    public static BigDecimal toNearestHalfDay(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        return amount.divide(HALF_DAY, 0, RoundingMode.HALF_UP)
                .multiply(HALF_DAY)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
