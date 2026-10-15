package com.kloudspot.ems.service;

import com.kloudspot.ems.dto.EmployeeRequest;
import com.kloudspot.ems.exception.DuplicateResourceException;
import com.kloudspot.ems.exception.ResourceNotFoundException;
import com.kloudspot.ems.model.Employee;
import com.kloudspot.ems.model.EmploymentStatus;
import com.kloudspot.ems.repository.EmployeeRepository;
import com.kloudspot.ems.repository.EmployeeSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class EmployeeService {

    private final EmployeeRepository repository;

    public EmployeeService(EmployeeRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<Employee> search(String search, String department, EmploymentStatus status, Pageable pageable) {
        return repository.findAll(EmployeeSpecifications.filter(search, department, status), pageable);
    }

    @Transactional(readOnly = true)
    public List<Employee> searchAll(String search, String department, EmploymentStatus status) {
        return repository.findAll(EmployeeSpecifications.filter(search, department, status),
                Sort.by(Sort.Direction.ASC, "id"));
    }

    @Transactional(readOnly = true)
    public Employee getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id " + id));
    }

    public Employee create(EmployeeRequest request) {
        if (repository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateResourceException("Employee with email " + request.email() + " already exists");
        }
        Employee employee = new Employee();
        apply(employee, request);
        // temporary business id, replaced once the DB generates the primary key
        employee.setEmployeeId("TMP" + UUID.randomUUID().toString().substring(0, 8));
        Employee saved = repository.save(employee);
        saved.setEmployeeId(String.format("EMP%04d", saved.getId()));
        return repository.save(saved);
    }

    public Employee update(Long id, EmployeeRequest request) {
        Employee employee = getById(id);
        if (!employee.getEmail().equalsIgnoreCase(request.email())
                && repository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateResourceException("Employee with email " + request.email() + " already exists");
        }
        apply(employee, request);
        return repository.save(employee);
    }

    public void delete(Long id) {
        Employee employee = getById(id);
        repository.delete(employee);
    }

    @Transactional(readOnly = true)
    public List<String> getDepartments() {
        return repository.findDistinctDepartments();
    }

    /** Generates CSV text for the given employees. */
    public String toCsv(List<Employee> employees) {
        StringBuilder sb = new StringBuilder();
        sb.append("Employee ID,Full Name,Email,Phone,Department,Designation,Date of Joining,Salary,Status\n");
        for (Employee e : employees) {
            sb.append(csv(e.getEmployeeId())).append(',')
              .append(csv(e.getFullName())).append(',')
              .append(csv(e.getEmail())).append(',')
              .append(csv(e.getPhoneNumber())).append(',')
              .append(csv(e.getDepartment())).append(',')
              .append(csv(e.getDesignation())).append(',')
              .append(csv(String.valueOf(e.getDateOfJoining()))).append(',')
              .append(csv(String.valueOf(e.getSalary()))).append(',')
              .append(csv(String.valueOf(e.getStatus()))).append('\n');
        }
        return sb.toString();
    }

    private String csv(String value) {
        if (value == null) return "";
        String v = value;
        // guard against CSV/Excel formula injection
        if (!v.isEmpty() && "=+-@".indexOf(v.charAt(0)) >= 0 && !v.matches("^[+-]?[0-9.]+$")) {
            v = "'" + v;
        }
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    private void apply(Employee e, EmployeeRequest r) {
        e.setFullName(r.fullName().trim());
        e.setEmail(r.email().trim().toLowerCase());
        e.setPhoneNumber(r.phoneNumber().trim());
        e.setDepartment(r.department().trim());
        e.setDesignation(r.designation().trim());
        e.setDateOfJoining(r.dateOfJoining());
        e.setSalary(r.salary());
        e.setStatus(r.status());
    }
}
