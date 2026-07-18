package com.practicallimits.spring_template.staff;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TerminationResult {
    private Staff staff;
    private List<Staff> staffWithNoApprover;
}
