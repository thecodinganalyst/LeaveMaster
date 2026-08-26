package com.practical.leavemaster.staff;

/**
 * Stable employment classification stored on Staff.
 *
 * <p>The value is intentionally jurisdiction-agnostic and may be absent for legacy records.</p>
 */
public enum EmploymentType {
    FULL_TIME,
    PART_TIME,
    CASUAL,
    CONTRACT,
    INTERN
}
