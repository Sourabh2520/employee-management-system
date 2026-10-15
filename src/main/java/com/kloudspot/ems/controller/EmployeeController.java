package com.kloudspot.ems.controller;

import com.kloudspot.ems.dto.EmployeeRequest;
import com.kloudspot.ems.dto.PageResponse;
import com.kloudspot.ems.model.Employee;
import com.kloudspot.ems.model.EmploymentStatus;
import com.kloudspot.ems.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/employees")
@Tag(name = "Employees", description = "Employee CRUD, search, filter, pagination and CSV export")
public class EmployeeController {

    private static final Set<String> SORTABLE = Set.of(
            "id", "employeeId", "fullName", "email", "department", "designation",
            "dateOfJoining", "salary", "status");

    private final EmployeeService service;

    public EmployeeController(EmployeeService service) {
        this.service = service;
    }

    @Operation(summary = "List employees with search, filters and pagination")
    @GetMapping
    public PageResponse<Employee> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) EmploymentStatus status,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        String sortField = SORTABLE.contains(sortBy) ? sortBy : "id";
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(dir, sortField));
        return PageResponse.from(service.search(search, department, status, pageable));
    }

    @Operation(summary = "Get employee by id")
    @GetMapping("/{id}")
    public Employee get(@PathVariable Long id) {
        return service.getById(id);
    }

    @Operation(summary = "Create employee")
    @PostMapping
    public ResponseEntity<Employee> create(@Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @Operation(summary = "Update employee")
    @PutMapping("/{id}")
    public Employee update(@PathVariable Long id, @Valid @RequestBody EmployeeRequest request) {
        return service.update(id, request);
    }

    @Operation(summary = "Delete employee")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "List distinct departments (used by the filter dropdown)")
    @GetMapping("/departments")
    public List<String> departments() {
        return service.getDepartments();
    }

    @Operation(summary = "Export employees (respecting search/filters) as CSV")
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) EmploymentStatus status) {
        String csv = service.toCsv(service.searchAll(search, department, status));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=employees.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }
}
