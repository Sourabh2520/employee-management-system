package com.kloudspot.ems.service;

import com.kloudspot.ems.dto.EmployeeRequest;
import com.kloudspot.ems.exception.DuplicateResourceException;
import com.kloudspot.ems.exception.ResourceNotFoundException;
import com.kloudspot.ems.model.Employee;
import com.kloudspot.ems.model.EmploymentStatus;
import com.kloudspot.ems.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository repository;

    @InjectMocks
    private EmployeeService service;

    private EmployeeRequest request;

    @BeforeEach
    void setUp() {
        request = new EmployeeRequest("  John Doe ", "John.Doe@Example.com", "9999999999",
                "Engineering", "Developer", LocalDate.of(2023, 1, 15),
                new BigDecimal("500000"), EmploymentStatus.ACTIVE);
    }

    private Employee existing(Long id, String email) {
        Employee e = new Employee();
        e.setId(id);
        e.setEmployeeId(String.format("EMP%04d", id));
        e.setFullName("Existing User");
        e.setEmail(email);
        e.setPhoneNumber("1234567890");
        e.setDepartment("HR");
        e.setDesignation("Manager");
        e.setDateOfJoining(LocalDate.of(2020, 1, 1));
        e.setSalary(new BigDecimal("100000"));
        e.setStatus(EmploymentStatus.ACTIVE);
        return e;
    }

    @Test
    void create_savesEmployeeAndGeneratesBusinessId() {
        when(repository.existsByEmailIgnoreCase("John.Doe@Example.com")).thenReturn(false);
        when(repository.save(any(Employee.class))).thenAnswer(inv -> {
            Employee e = inv.getArgument(0);
            if (e.getId() == null) e.setId(7L);
            return e;
        });

        Employee result = service.create(request);

        assertEquals("EMP0007", result.getEmployeeId());
        assertEquals("John Doe", result.getFullName());
        assertEquals("john.doe@example.com", result.getEmail());
        assertEquals(EmploymentStatus.ACTIVE, result.getStatus());
        verify(repository, times(2)).save(any(Employee.class));
    }

    @Test
    void create_duplicateEmail_throwsAndDoesNotSave() {
        when(repository.existsByEmailIgnoreCase(anyString())).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> service.create(request));
        verify(repository, never()).save(any());
    }

    @Test
    void getById_found_returnsEmployee() {
        Employee e = existing(1L, "a@b.com");
        when(repository.findById(1L)).thenReturn(Optional.of(e));

        assertSame(e, service.getById(1L));
    }

    @Test
    void getById_missing_throwsNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getById(99L));
    }

    @Test
    void update_changesFields() {
        Employee e = existing(1L, "john.doe@example.com");
        when(repository.findById(1L)).thenReturn(Optional.of(e));
        when(repository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        Employee result = service.update(1L, request);

        assertEquals("John Doe", result.getFullName());
        assertEquals("Engineering", result.getDepartment());
        assertEquals(new BigDecimal("500000"), result.getSalary());
        // email unchanged -> no duplicate check needed
        verify(repository, never()).existsByEmailIgnoreCase(anyString());
    }

    @Test
    void update_emailTakenByAnotherEmployee_throwsDuplicate() {
        Employee e = existing(1L, "old@example.com");
        when(repository.findById(1L)).thenReturn(Optional.of(e));
        when(repository.existsByEmailIgnoreCase("John.Doe@Example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> service.update(1L, request));
        verify(repository, never()).save(any());
    }

    @Test
    void update_missingEmployee_throwsNotFound() {
        when(repository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.update(5L, request));
    }

    @Test
    void delete_existing_deletesEntity() {
        Employee e = existing(1L, "a@b.com");
        when(repository.findById(1L)).thenReturn(Optional.of(e));

        service.delete(1L);

        verify(repository).delete(e);
    }

    @Test
    void delete_missing_throwsNotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete(1L));
        verify(repository, never()).delete(any(Employee.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_returnsPageFromRepository() {
        Page<Employee> page = new PageImpl<>(List.of(existing(1L, "a@b.com")));
        when(repository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);

        Page<Employee> result = service.search("john", "HR", EmploymentStatus.ACTIVE, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getDepartments_delegatesToRepository() {
        when(repository.findDistinctDepartments()).thenReturn(List.of("HR", "Sales"));

        assertEquals(List.of("HR", "Sales"), service.getDepartments());
    }

    @Test
    void toCsv_escapesQuotesAndFormulaInjection() {
        Employee e = existing(1L, "a@b.com");
        e.setFullName("=cmd|' /C calc'!A0");
        e.setDesignation("Lead \"Senior\"");

        String csv = service.toCsv(List.of(e));

        assertTrue(csv.startsWith("Employee ID,Full Name"));
        assertTrue(csv.contains("\"'=cmd"));
        assertTrue(csv.contains("\"Lead \"\"Senior\"\"\""));
    }
}
