package com.veely.service;

import com.veely.entity.Document;
import com.veely.entity.Employee;
import com.veely.entity.ExpenseReport;
import com.veely.entity.FuelCard;
import com.veely.repository.ComplianceItemRepository;
import com.veely.repository.DocumentRepository;
import com.veely.repository.EmployeeRepository;
import com.veely.repository.FuelCardRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;
import java.util.HashSet;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepo;
    @Mock
    private DocumentRepository documentRepo;
    @Mock
    private ComplianceItemRepository complianceItemRepo;
    @Mock
    private FuelCardRepository fuelCardRepo;
    @Mock
    private FileSystemStorageService fileStorage;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private PdfEmployeeService pdfEmployeeService;
    @Mock
    private ExpenseReportService expenseReportService;

    @InjectMocks
    private EmployeeService employeeService;

    @Test
    void deleteShouldUnlinkFuelCardsRemoveExpenseReportsAndComplianceItems() {
        Employee employee = Employee.builder()
                .id(1L)
                .personalDocuments(new HashSet<Document>())
                .build();
        FuelCard card = FuelCard.builder().id(2L).employee(employee).build();
        ExpenseReport report = ExpenseReport.builder().id(3L).build();
        when(employeeRepo.findById(1L)).thenReturn(Optional.of(employee));
        when(documentRepo.findByEmployeeId(1L)).thenReturn(Collections.emptyList());
        when(fuelCardRepo.findByEmployeeId(1L)).thenReturn(card);
        when(expenseReportService.findByEmployeeId(1L)).thenReturn(Collections.singletonList(report));

        employeeService.delete(1L);

        verify(fuelCardRepo).findByEmployeeId(1L);
        verify(fuelCardRepo).save(card);
        verify(expenseReportService).findByEmployeeId(1L);
        verify(expenseReportService).delete(3L);
        verify(complianceItemRepo).deleteByEmployeeId(1L);
        verify(employeeRepo).delete(employee);
    }
}
