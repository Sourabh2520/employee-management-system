package com.kloudspot.ems.config;

import com.kloudspot.ems.dto.EmployeeRequest;
import com.kloudspot.ems.model.EmploymentStatus;
import com.kloudspot.ems.repository.EmployeeRepository;
import com.kloudspot.ems.service.EmployeeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Inserts demo data on first start (when the table is empty). Disable with app.seed-data=false. */
@Component
public class DataSeeder implements CommandLineRunner {

    private final EmployeeService service;
    private final EmployeeRepository repository;
    private final boolean enabled;

    public DataSeeder(EmployeeService service, EmployeeRepository repository,
                      @Value("${app.seed-data:true}") boolean enabled) {
        this.service = service;
        this.repository = repository;
        this.enabled = enabled;
    }

    @Override
    public void run(String... args) {
        if (!enabled || repository.count() > 0) return;
        add("Aarav Sharma", "aarav.sharma@example.com", "9876543210", "Engineering", "Software Engineer", "2022-06-15", "850000", EmploymentStatus.ACTIVE);
        add("Priya Patel", "priya.patel@example.com", "9876543211", "Engineering", "Senior Developer", "2020-01-10", "1500000", EmploymentStatus.ACTIVE);
        add("Rohan Mehta", "rohan.mehta@example.com", "9876543212", "HR", "HR Manager", "2019-03-21", "1100000", EmploymentStatus.ACTIVE);
        add("Sneha Iyer", "sneha.iyer@example.com", "9876543213", "Finance", "Accountant", "2021-09-01", "700000", EmploymentStatus.ACTIVE);
        add("Vikram Singh", "vikram.singh@example.com", "9876543214", "Sales", "Sales Executive", "2023-02-14", "600000", EmploymentStatus.INACTIVE);
        add("Ananya Reddy", "ananya.reddy@example.com", "9876543215", "Marketing", "Marketing Lead", "2020-11-30", "1200000", EmploymentStatus.ACTIVE);
        add("Karan Desai", "karan.desai@example.com", "9876543216", "Engineering", "QA Engineer", "2022-08-08", "650000", EmploymentStatus.ACTIVE);
        add("Neha Gupta", "neha.gupta@example.com", "9876543217", "HR", "Recruiter", "2023-05-19", "500000", EmploymentStatus.ACTIVE);
        add("Arjun Nair", "arjun.nair@example.com", "9876543218", "Finance", "Finance Analyst", "2021-04-05", "800000", EmploymentStatus.INACTIVE);
        add("Meera Joshi", "meera.joshi@example.com", "9876543219", "Sales", "Sales Manager", "2018-12-12", "1400000", EmploymentStatus.ACTIVE);
        add("Rahul Verma", "rahul.verma@example.com", "9876543220", "Engineering", "DevOps Engineer", "2022-01-03", "950000", EmploymentStatus.ACTIVE);
        add("Divya Kulkarni", "divya.kulkarni@example.com", "9876543221", "Marketing", "Content Writer", "2023-07-24", "450000", EmploymentStatus.ACTIVE);
    }

    private void add(String name, String email, String phone, String dept, String desig,
                     String date, String salary, EmploymentStatus status) {
        service.create(new EmployeeRequest(name, email, phone, dept, desig,
                LocalDate.parse(date), new BigDecimal(salary), status));
    }
}
