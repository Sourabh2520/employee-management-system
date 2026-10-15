package com.kloudspot.ems.dto;

import com.kloudspot.ems.model.EmploymentStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record EmployeeRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name must be at most 100 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email format is invalid")
        @Size(max = 150, message = "Email must be at most 150 characters")
        String email,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^[0-9+\\-\\s]{7,15}$", message = "Phone number must be 7-15 digits (may include + - and spaces)")
        String phoneNumber,

        @NotBlank(message = "Department is required")
        @Size(max = 80)
        String department,

        @NotBlank(message = "Designation is required")
        @Size(max = 80)
        String designation,

        @NotNull(message = "Date of joining is required")
        LocalDate dateOfJoining,

        @NotNull(message = "Salary is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Salary must be greater than 0")
        @Digits(integer = 10, fraction = 2, message = "Salary format is invalid")
        BigDecimal salary,

        @NotNull(message = "Status is required (ACTIVE or INACTIVE)")
        EmploymentStatus status
) {}
