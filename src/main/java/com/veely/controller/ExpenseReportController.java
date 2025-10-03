package com.veely.controller;

import com.veely.entity.Employee;
import com.veely.entity.ExpenseItem;
import com.veely.entity.ExpenseReport;
import com.veely.entity.Supplier;
import com.veely.entity.Document;
import com.veely.model.ExpenseStatus;
import com.veely.service.EmployeeService;
import com.veely.service.ExpenseReportService;
import com.veely.service.SupplierService;
import com.veely.service.DocumentService;
import com.veely.model.PaymentMethod;
import com.veely.service.ProjectService;
import com.veely.model.DocumentType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;


@Controller
@RequestMapping("/fleet/expense-reports")
@RequiredArgsConstructor
public class ExpenseReportController {

    private final ExpenseReportService reportService;
    private final EmployeeService employeeService;
    private final SupplierService supplierService;
    private final ProjectService projectService;
    private final DocumentService documentService;

    @GetMapping
    public String list(Model model, Authentication auth) {
        if (isAdminOrExpenseManager(auth)) {
            model.addAttribute("reports", reportService.findAll());
        } else {
            Employee current = employeeService.findByEmail(auth.getName());
            model.addAttribute("reports", reportService.findByEmployeeId(current.getId()));
        }
        return "fleet/expense_reports/index";
    }

    @GetMapping("/new")
    public String newForm(Model model, Authentication auth) {
        List<Employee> employees;
        ExpenseReport report = new ExpenseReport();
        String baseNum = reportService.getNextExpenseReportBase();
        if (isAdminOrExpenseManager(auth)) {
            employees = employeeService.findAll();
        } else {
            Employee current = employeeService.findByEmail(auth.getName());
            employees = java.util.List.of(current);
            report.setEmployee(current);
        }
        if (!employees.isEmpty()) {
            report.setExpenseReportNum(baseNum + getInitials(employees.get(0)));
        } else {
            report.setExpenseReportNum(baseNum);
        }
        model.addAttribute("report", report);
        model.addAttribute("baseReportNum", baseNum);
        model.addAttribute("items", new ArrayList<ExpenseItem>());
        model.addAttribute("employees", employees);
        model.addAttribute("statuses", ExpenseStatus.values());
        model.addAttribute("paymentMethods", PaymentMethod.values());
        model.addAttribute("suppliers", supplierService.findAll());
        model.addAttribute("projects", projectService.findAll());
        
        model.addAttribute("docTypes", new DocumentType[]{DocumentType.INVOICE, DocumentType.RECEIPT, DocumentType.OTHER});
        return "fleet/expense_reports/form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("report") ExpenseReport report, BindingResult binding,
	    		@RequestParam(value = "itemId", required = false) List<String> itemId,
	            @RequestParam(value = "itemDesc", required = false) List<String> itemDesc,
	            @RequestParam(value = "itemAmount", required = false) List<String> itemAmount,
	            @RequestParam(value = "itemDate", required = false) List<String> itemDate,
	            @RequestParam(value = "itemInvoice", required = false) List<String> itemInvoice,
	            @RequestParam(value = "itemSupplierId", required = false) List<String> itemSupplier,
	            @RequestParam(value = "itemNote", required = false) List<String> itemNote,
	            Model model,
	            Authentication auth) {
        if (binding.hasErrors()) {
        	List<Employee> employees = isAdminOrExpenseManager(auth)
                    ? employeeService.findAll()
                    : java.util.List.of(employeeService.findByEmail(auth.getName()));
            model.addAttribute("employees", employees);
            model.addAttribute("statuses", ExpenseStatus.values());
            model.addAttribute("paymentMethods", PaymentMethod.values());
            model.addAttribute("suppliers", supplierService.findAll());
            model.addAttribute("projects", projectService.findAll());
            model.addAttribute("docTypes", new DocumentType[]{DocumentType.INVOICE, DocumentType.RECEIPT, DocumentType.OTHER});
            return "fleet/expense_reports/form";
        }
        
        List<ExpenseItem> items = buildItems(itemId, itemDesc, itemAmount, itemDate, itemInvoice, itemSupplier, itemNote);
        if (items.isEmpty()) {
            List<Employee> employees = isAdminOrExpenseManager(auth)
                    ? employeeService.findAll()
                    : java.util.List.of(employeeService.findByEmail(auth.getName()));
            model.addAttribute("employees", employees);
            model.addAttribute("statuses", ExpenseStatus.values());
            model.addAttribute("paymentMethods", PaymentMethod.values());
            model.addAttribute("suppliers", supplierService.findAll());
            model.addAttribute("projects", projectService.findAll());
            model.addAttribute("items", new ArrayList<ExpenseItem>());
            model.addAttribute("docTypes", new DocumentType[]{DocumentType.INVOICE, DocumentType.RECEIPT, DocumentType.OTHER});
            model.addAttribute("baseReportNum", reportService.getNextExpenseReportBase());
            model.addAttribute("errorMessage", "Aggiungi almeno una voce di spesa");
            return "fleet/expense_reports/form";
        }
        
        ExpenseReport saved = reportService.create(report, items);
        return "redirect:/fleet/expense-reports/" + saved.getId() + "/edit";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        ExpenseReport r = reportService.findByIdOrThrow(id);
        List<ExpenseItem> items = reportService.findItems(id);
        Map<Long, List<com.veely.model.DocumentInfo>> docs = new java.util.HashMap<>();
        for (ExpenseItem it : items) {
        	docs.put(it.getId(), documentService.getExpenseItemDocumentInfo(it.getId()));
        }
        model.addAttribute("report", r);
        model.addAttribute("items", items);
        model.addAttribute("itemDocs", docs);
        model.addAttribute("employees", employeeService.findAll());
        model.addAttribute("statuses", ExpenseStatus.values());
        model.addAttribute("paymentMethods", PaymentMethod.values());
        model.addAttribute("suppliers", supplierService.findAll());
        model.addAttribute("projects", projectService.findAll());
        model.addAttribute("docTypes", new DocumentType[]{DocumentType.INVOICE, DocumentType.RECEIPT, DocumentType.OTHER});
        return "fleet/expense_reports/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("report") ExpenseReport report,
                         BindingResult binding,
                         @RequestParam(value = "itemId", required = false) List<String> itemId,
                         @RequestParam(value = "itemDesc", required = false) List<String> itemDesc,
                         @RequestParam(value = "itemAmount", required = false) List<String> itemAmount,
                         @RequestParam(value = "itemDate", required = false) List<String> itemDate,
                         @RequestParam(value = "itemInvoice", required = false) List<String> itemInvoice,
                         @RequestParam(value = "itemSupplierId", required = false) List<String> itemSupplier,
                         @RequestParam(value = "itemNote", required = false) List<String> itemNote,
                         Model model) {
        if (binding.hasErrors()) {
            model.addAttribute("employees", employeeService.findAll());
            model.addAttribute("statuses", ExpenseStatus.values());
            model.addAttribute("paymentMethods", PaymentMethod.values());
            model.addAttribute("suppliers", supplierService.findAll());
            model.addAttribute("projects", projectService.findAll());
            List<ExpenseItem> items = reportService.findItems(id);
            Map<Long, List<com.veely.entity.Document>> itemDocs = new HashMap<>();
            for (ExpenseItem it : items) {
                itemDocs.put(it.getId(), documentService.getExpenseItemDocuments(it.getId()));
            }
            model.addAttribute("items", items);
            model.addAttribute("itemDocs", itemDocs);
            model.addAttribute("docTypes", new DocumentType[]{DocumentType.INVOICE, DocumentType.RECEIPT, DocumentType.OTHER});
            return "fleet/expense_reports/form";
        }
        List<ExpenseItem> items = buildItems(itemId, itemDesc, itemAmount, itemDate, itemInvoice, itemSupplier, itemNote);
        if (items.isEmpty()) {
            model.addAttribute("employees", employeeService.findAll());
            model.addAttribute("statuses", ExpenseStatus.values());
            model.addAttribute("paymentMethods", PaymentMethod.values());
            model.addAttribute("suppliers", supplierService.findAll());
            model.addAttribute("projects", projectService.findAll());
            List<ExpenseItem> existing = reportService.findItems(id);
            Map<Long, List<com.veely.entity.Document>> itemDocs = new HashMap<>();
            for (ExpenseItem it : existing) {
                itemDocs.put(it.getId(), documentService.getExpenseItemDocuments(it.getId()));
            }
            model.addAttribute("items", existing);
            model.addAttribute("itemDocs", itemDocs);
            model.addAttribute("docTypes", new DocumentType[]{DocumentType.INVOICE, DocumentType.RECEIPT, DocumentType.OTHER});
            model.addAttribute("errorMessage", "Aggiungi almeno una voce di spesa");
            return "fleet/expense_reports/form";
        }

        reportService.update(id, report, items);
        return "redirect:/fleet/expense-reports/" + id + "/edit";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        reportService.delete(id);
        return "redirect:/fleet/expense-reports";
    }

    @PostMapping("/{id}/approve")
    public String toggleApprove(@PathVariable Long id) {
    	reportService.toggleApproval(id);
        return "redirect:/fleet/expense-reports/" + id + "/edit";
    }
    
    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long id) throws IOException {
        byte[] pdf = reportService.exportPdf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=expense-report-" + id + ".pdf")
                .body(pdf);
    }
    
    @GetMapping("/docs/{docId}")
    public ResponseEntity<Resource> downloadItemDocument(@PathVariable Long docId, HttpServletRequest request) throws IOException {
        Resource res = documentService.loadDocument(docId);
        String ct = request.getServletContext().getMimeType(res.getFile().getAbsolutePath());
        if (ct == null) ct = "application/octet-stream";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(ct))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + res.getFilename() + "\"")
                .body(res);
    }

    @GetMapping("/items/{itemId}/docs/{docId}/delete")
    public String deleteItemDocument(@PathVariable Long itemId,
                                     @PathVariable Long docId) throws IOException {
        Document doc = documentService.getDocument(docId);
        Long reportId = doc.getExpenseItem().getExpenseReport().getId();
        documentService.deleteDocument(docId);
        return "redirect:/fleet/expense-reports/" + reportId + "/edit";
    }

    
    @PostMapping("/items/{itemId}/docs")
    public String uploadItemDocument(@PathVariable Long itemId,
                                     @RequestParam("file") MultipartFile file,
                                     @RequestParam("type") DocumentType type,
                                     @RequestParam(value = "issueDate", required = false) String issueDate,
                                     @RequestParam(value = "expiryDate", required = false) String expiryDate,
                                     @RequestParam("reportId") Long reportId) throws IOException {
        LocalDate issued = (issueDate == null || issueDate.isBlank()) ? null : LocalDate.parse(issueDate);
        LocalDate exp = (expiryDate == null || expiryDate.isBlank()) ? null : LocalDate.parse(expiryDate);
        documentService.uploadExpenseItemDocument(itemId, file, type, issued, exp);
        return "redirect:/fleet/expense-reports/" + reportId + "/edit";
    }

    private List<ExpenseItem> buildItems(List<String> ids, List<String> descs, List<String> amounts, List<String> dates,
            List<String> invoices, List<String> suppliers, List<String> notes) {
        List<ExpenseItem> list = new ArrayList<>();
        if (descs == null) {
            return list;
        }
        int size = descs.size();
        for (int i = 0; i < size; i++) {
        	String idStr = getValue(ids, i);
            String desc = getValue(descs, i);
            String amountStr = getValue(amounts, i);
            String dateStr = getValue(dates, i);
            String invoice = getValue(invoices, i);
            String supplierId = getValue(suppliers, i);
            String note = getValue(notes, i);

            if ((desc == null || desc.isBlank()) &&
                (amountStr == null || amountStr.isBlank()) &&
                (dateStr == null || dateStr.isBlank()) &&
                (invoice == null || invoice.isBlank()) &&
                (supplierId == null || supplierId.isBlank()) &&
                (note == null || note.isBlank())) {
                continue;
            }
            ExpenseItem it = new ExpenseItem();
            if (idStr != null && !idStr.isBlank()) {
                it.setId(Long.parseLong(idStr));
            }
            it.setDescription(desc);
            if (amountStr != null && !amountStr.isBlank()) {
                it.setAmount(new BigDecimal(amountStr.replace(',', '.')));
            }
            if (dateStr != null && !dateStr.isBlank()) {
                it.setDate(LocalDate.parse(dateStr));
            }
            if (invoice != null && !invoice.isBlank()) {
                it.setInvoiceNumber(invoice);
            }
            if (supplierId != null && !supplierId.isBlank()) {
                Supplier s = supplierService.findByIdOrThrow(Long.parseLong(supplierId));
                it.setSupplier(s);
            }
            if (note != null && !note.isBlank()) {
                it.setNote(note);
            }
            list.add(it);
        }
        return list;
    }
    
    private String getValue(List<String> list, int index) {
        return (list != null && index < list.size()) ? list.get(index) : null;
    }
    
    private String getInitials(Employee e) {
        if (e == null) {
            return "";
        }
        String first = e.getFirstName() == null ? "" : e.getFirstName();
        String last = e.getLastName() == null ? "" : e.getLastName();
        char li = last.isEmpty() ? ' ' : Character.toUpperCase(last.charAt(0));
        char fi = first.isEmpty() ? ' ' : Character.toUpperCase(first.charAt(0));
        return "" + li + fi;
    }
    
    private boolean isAdminOrExpenseManager(Authentication auth) {
        if (auth == null) return false;
        return auth.getAuthorities().stream().anyMatch(a ->
            a.getAuthority().equals("ROLE_Administrator") ||
            a.getAuthority().equals("ROLE_Expense Manager"));
    }
    
    @GetMapping("/api/filter")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> filterReports(
            @RequestParam(required = false) String employee,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication auth) {
        
        List<ExpenseReport> filteredReports = reportService.findFiltered(employee, status, startDate, endDate);
        
        if (!isAdminOrExpenseManager(auth)) {
            Employee current = employeeService.findByEmail(auth.getName());
            filteredReports = filteredReports.stream()
                    .filter(r -> r.getEmployee().getId().equals(current.getId()))
                    .collect(Collectors.toList());
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("reports", filteredReports);
        response.put("count", filteredReports.size());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/employees")
    @ResponseBody
    public ResponseEntity<List<String>> getEmployees(Authentication auth) {
        List<String> employees;
        if (isAdminOrExpenseManager(auth)) {
            employees = reportService.findAll().stream()
                    .map(r -> r.getEmployee().getLastName() + " " + r.getEmployee().getFirstName())
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
        } else {
            Employee current = employeeService.findByEmail(auth.getName());
            employees = java.util.List.of(current.getLastName() + " " + current.getFirstName());
        }
        
        return ResponseEntity.ok(employees);
    }
}
