package com.veely.controller;

import com.veely.entity.ExpenseItem;
import com.veely.service.DocumentService;
import com.veely.service.EmployeeService;
import com.veely.service.ExpenseReportService;
import com.veely.service.ProjectService;
import com.veely.service.SupplierService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ExpenseReportControllerTest {

    @SuppressWarnings("unchecked")
    @Test
    void buildItemsShouldHandleMissingParams() throws Exception {
        ExpenseReportController controller = new ExpenseReportController(
                mock(ExpenseReportService.class),
                mock(EmployeeService.class),
                mock(SupplierService.class),
                mock(ProjectService.class),
                mock(DocumentService.class)
        );

        List<String> ids = Collections.emptyList();
        List<String> descs = List.of("Taxi");
        List<String> amounts = List.of("10.00");
        List<String> dates = List.of("2024-01-01");
        List<String> invoices = Collections.emptyList();
        List<String> suppliers = Collections.emptyList();
        List<String> notes = Collections.emptyList();

        Method m = ExpenseReportController.class.getDeclaredMethod("buildItems", List.class, List.class, List.class, List.class, List.class, List.class, List.class);
        m.setAccessible(true);
        List<ExpenseItem> items = (List<ExpenseItem>) m.invoke(controller, ids, descs, amounts, dates, invoices, suppliers, notes);

        assertThat(items).hasSize(1);
        ExpenseItem item = items.get(0);
        assertThat(item.getDescription()).isEqualTo("Taxi");
        assertThat(item.getAmount()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(item.getDate()).isEqualTo(LocalDate.parse("2024-01-01"));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    void buildItemsShouldReturnEmptyListWhenNoDescriptions() throws Exception {
        ExpenseReportController controller = new ExpenseReportController(
                mock(ExpenseReportService.class),
                mock(EmployeeService.class),
                mock(SupplierService.class),
                mock(ProjectService.class),
                mock(DocumentService.class)
        );

        Method m = ExpenseReportController.class.getDeclaredMethod("buildItems", List.class, List.class, List.class, List.class, List.class, List.class, List.class);
        m.setAccessible(true);
        List<ExpenseItem> items = (List<ExpenseItem>) m.invoke(controller, null, null, null, null, null, null, null);

        assertThat(items).isEmpty();
    }
}
