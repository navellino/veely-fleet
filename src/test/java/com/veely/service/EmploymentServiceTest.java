package com.veely.service;
import com.veely.entity.Employment;
import com.veely.model.EmploymentStatus;
import com.veely.repository.DocumentRepository;
import com.veely.repository.EmployeeRepository;
import com.veely.repository.EmploymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmploymentServiceTest {

    @Mock
    private EmploymentRepository employmentRepo;
    @Mock
    private DocumentRepository documentRepo;
    @Mock
    private FileSystemStorageService fileStorage;
    @Mock
    private EmployeeService employeeService;
    @Mock
    private EmployeeRepository employeeRepo;
    @Mock
    private PdfEmploymentService pdfEmploymentService;

    @InjectMocks
    private EmploymentService employmentService;

    @Test
    void createShouldTerminateWhenEndDatePast() {
        Employment employment = Employment.builder()
            .matricola("MAT1")
            .endDate(LocalDate.now().minusDays(1))
            .status(EmploymentStatus.ACTIVE)
            .build();

        when(employmentRepo.save(any(Employment.class))).thenAnswer(inv -> inv.getArgument(0));

        Employment saved = employmentService.create(employment);

        assertThat(saved.getStatus()).isEqualTo(EmploymentStatus.TERMINATED);
    }

    @Test
    void updateShouldTerminateWhenEndDatePast() {
        Employment existing = Employment.builder()
            .id(1L)
            .status(EmploymentStatus.ACTIVE)
            .build();
        when(employmentRepo.findById(1L)).thenReturn(Optional.of(existing));

        Employment payload = Employment.builder()
            .endDate(LocalDate.now().minusDays(2))
            .status(EmploymentStatus.ACTIVE)
            .build();

        Employment updated = employmentService.update(1L, payload);

        assertThat(updated.getStatus()).isEqualTo(EmploymentStatus.TERMINATED);
    }
}
