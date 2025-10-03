package com.veely.service;

import com.veely.entity.CompanyInfo;
import com.veely.entity.ExpenseItem;
import com.veely.entity.ExpenseReport;
import com.veely.exception.ResourceNotFoundException;
import com.veely.model.ExpenseStatus;
import com.veely.repository.EmployeeRepository;
import com.veely.repository.ExpenseItemRepository;
import com.veely.repository.ExpenseReportRepository;
import com.veely.repository.ProjectRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.awt.Color;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ExpenseReportService {

    private final ExpenseReportRepository reportRepo;
    private final ExpenseItemRepository itemRepo;
    private final EmployeeRepository employeeRepo;
    private final ProjectRepository projectRepo;
    private final DocumentService documentService;
    private final CompanyInfoService companyInfoService; // Aggiungi questa dipendenza


    public ExpenseReport create(ExpenseReport report, List<ExpenseItem> items) {
    	 if (report.getEmployee() != null && report.getEmployee().getId() != null) {
             employeeRepo.findById(report.getEmployee().getId()).ifPresent(report::setEmployee);
         }
         if (report.getProject() != null && report.getProject().getId() != null) {
             projectRepo.findById(report.getProject().getId()).ifPresent(report::setProject);
         } else {
             report.setProject(null);
         }
        report.setExpenseStatus(ExpenseStatus.Draft);
        report.setExpenseReportTotal(sumItems(items));
        if (report.getReimbursableTotal() == null) {
            report.setReimbursableTotal(java.math.BigDecimal.ZERO);
        }
        report.setNonReimbursableTotal(report.getExpenseReportTotal().subtract(report.getReimbursableTotal()));
        ExpenseReport saved = reportRepo.save(report);
        for (ExpenseItem item : items) {
            item.setExpenseReport(saved);
            itemRepo.save(item);
        }
        return saved;
    }

    public ExpenseReport update(Long id, ExpenseReport payload, List<ExpenseItem> items) {
        ExpenseReport existing = findByIdOrThrow(id);
        existing.setPuorpose(payload.getPuorpose());
        existing.setStartDate(payload.getStartDate());
        existing.setEndDate(payload.getEndDate());
        existing.setPaymentMethodCode(payload.getPaymentMethodCode());
        existing.setExpenseStatus(payload.getExpenseStatus());
        existing.setExpenseReportNum(payload.getExpenseReportNum());
        existing.setCreationDate(payload.getCreationDate());
        if (payload.getEmployee() != null && payload.getEmployee().getId() != null) {
            employeeRepo.findById(payload.getEmployee().getId()).ifPresent(existing::setEmployee);
        } else {
            existing.setEmployee(null);
        }
        if (payload.getProject() != null && payload.getProject().getId() != null) {
            projectRepo.findById(payload.getProject().getId()).ifPresent(existing::setProject);
        } else {
            existing.setProject(null);
        }
        existing.setReimbursableTotal(payload.getReimbursableTotal());
        existing.setExpenseReportTotal(sumItems(items));
        if (existing.getReimbursableTotal() == null) {
            existing.setReimbursableTotal(java.math.BigDecimal.ZERO);
        }
        existing.setNonReimbursableTotal(existing.getExpenseReportTotal().subtract(existing.getReimbursableTotal()));

        List<ExpenseItem> current = itemRepo.findByExpenseReportId(id);
        Map<Long, ExpenseItem> currentMap = current.stream()
                .collect(java.util.stream.Collectors.toMap(ExpenseItem::getId, java.util.function.Function.identity()));
        for (ExpenseItem item : items) {
        	if (item.getId() != null && currentMap.containsKey(item.getId())) {
                ExpenseItem existingItem = currentMap.remove(item.getId());
                existingItem.setDescription(item.getDescription());
                existingItem.setAmount(item.getAmount());
                existingItem.setDate(item.getDate());
                existingItem.setInvoiceNumber(item.getInvoiceNumber());
                existingItem.setSupplier(item.getSupplier());
                existingItem.setNote(item.getNote());
                itemRepo.save(existingItem);
            } else {
                item.setExpenseReport(existing);
                itemRepo.save(item);
            }
        }

        for (ExpenseItem toRemove : currentMap.values()) {
            documentService.deleteExpenseItemDocuments(toRemove.getId());
            itemRepo.delete(toRemove);
        }
        return existing;
    }

    @Transactional(readOnly = true)
    public ExpenseReport findByIdOrThrow(Long id) {
        return reportRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nota spese non trovata: " + id));
    }

    @Transactional(readOnly = true)
    public List<ExpenseReport> findAll() {
        return reportRepo.findAll();
    }

    @Transactional(readOnly = true)
    public List<ExpenseReport> findByEmployeeId(Long employeeId) {
        return reportRepo.findByEmployeeId(employeeId);
    }
    
    @Transactional(readOnly = true)
    public List<ExpenseItem> findItems(Long reportId) {
        return itemRepo.findByExpenseReportId(reportId);
    }
    
    @Transactional(readOnly = true)
    public ExpenseItem findItemById(Long id) {
        return itemRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Voce spesa non trovata: " + id));
    }

    public void delete(Long id) {
        ExpenseReport r = findByIdOrThrow(id);
        int removedNumber = extractSequentialNumber(r.getExpenseReportNum());
        List<ExpenseItem> items = itemRepo.findByExpenseReportId(id);
        for (ExpenseItem it : items) {
            documentService.deleteExpenseItemDocuments(it.getId());
        }
        itemRepo.deleteAll(items);
        reportRepo.delete(r);
        renumberExpenseReports(removedNumber);
    }
    
    private BigDecimal sumItems(List<ExpenseItem> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (ExpenseItem i : items) {
            if (i.getAmount() != null) {
                total = total.add(i.getAmount());
            }
        }
        return total;
    }
    
    public ExpenseReport toggleApproval(Long id) {
        ExpenseReport r = findByIdOrThrow(id);
        if (r.getExpenseStatus() == ExpenseStatus.Approved) {
            r.setExpenseStatus(ExpenseStatus.Draft);
            r.setFinalApprovalDate(null);
        } else {
            r.setExpenseStatus(ExpenseStatus.Approved);
            r.setFinalApprovalDate(LocalDate.now());
        }
        return reportRepo.save(r);
    }
    
    @Transactional(readOnly = true)
    public String getNextExpenseReportBase() {
    	long next = reportRepo.count() + 1;
        int year = LocalDate.now().getYear();
        return String.format("%03d/%d/", next, year);
    }
    
    private int extractSequentialNumber(String expenseReportNum) {
        if (expenseReportNum == null || expenseReportNum.length() < 3) {
            return 0;
        }
        try {
            return Integer.parseInt(expenseReportNum.substring(0, 3));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private void renumberExpenseReports(int removedNumber) {
        List<ExpenseReport> reports = reportRepo.findAll();
        for (ExpenseReport er : reports) {
            int current = extractSequentialNumber(er.getExpenseReportNum());
            if (current > removedNumber) {
                String suffix = er.getExpenseReportNum().substring(3);
                er.setExpenseReportNum(String.format("%03d%s", current - 1, suffix));
            }
        }
        reportRepo.saveAll(reports);
    }
    
    public List<ExpenseReport> findFiltered(String employee, String status, LocalDate startDate, LocalDate endDate) {
	    return findAll().stream()
	            .filter(report -> {
	                // Filtra per dipendente
	                if (employee != null && !employee.isEmpty()) {
	                    String fullName = report.getEmployee().getLastName() + " " + report.getEmployee().getFirstName();
	                    if (!fullName.toLowerCase().contains(employee.toLowerCase())) {
	                        return false;
	                    }
	                }
	                
	                // Filtra per stato
	                if (status != null && !status.isEmpty()) {
	                    if (!report.getExpenseStatus().name().equalsIgnoreCase(status)) {
	                        return false;
	                    }
	                }
	                
	                // Filtra per data inizio
	                if (startDate != null) {
	                    if (report.getStartDate().isBefore(startDate)) {
	                        return false;
	                    }
	                }
	                
	                // Filtra per data fine
	                if (endDate != null) {
	                    if (report.getEndDate().isAfter(endDate)) {
	                        return false;
	                    }
	                }
	                
	                return true;
	            })
	            .collect(Collectors.toList());
	}

    /**
     * Genera la versione PDF di una singola nota spese.
     * @throws IOException 
     */
    /**
     * Genera la versione PDF di una singola nota spese con logo, watermark e branding aziendale.
     */
    @Transactional(readOnly = true)
    public byte[] exportPdf(Long id) throws IOException {
        ExpenseReport report = findByIdOrThrow(id);
        List<ExpenseItem> items = findItems(id);
        
        // Ottieni le informazioni aziendali
        Optional<CompanyInfo> companyInfoOpt = companyInfoService.getPrimaryCompanyInfoOptional();
        CompanyInfo companyInfo = companyInfoOpt.orElse(null);

        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        Document pdfDoc = new Document(PageSize.A4, 30, 30, 35, 35);
        
        try {
            PdfWriter writer = PdfWriter.getInstance(pdfDoc, out);
            pdfDoc.open();

            // === WATERMARK DI SFONDO ===
            addWatermarkIfEnabled(writer, companyInfo);

            // === HEADER SEMPLIFICATO: LOGO + RAGIONE SOCIALE ===
            addCompanyHeader(pdfDoc, companyInfo);

            // === LINEA SEPARATRICE ===
            addSeparatorLine(pdfDoc, companyInfo);

            // === TITOLO DOCUMENTO ===
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.decode("#333333"));
            Paragraph title = new Paragraph("NOTA SPESE", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20f);
            pdfDoc.add(title);

            // === INFORMAZIONI PRINCIPALI ===
            addReportInformation(pdfDoc, report, companyInfo);

            // === DETTAGLIO SPESE ===
            addExpenseDetails(pdfDoc, items, companyInfo);

            // === RIEPILOGO TOTALI ===
            addExpenseSummary(pdfDoc, report, companyInfo);

            // === FOOTER AZIENDALE CON TUTTE LE INFORMAZIONI ===
            addCompanyFooter(pdfDoc, report, companyInfo);

            pdfDoc.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Errore nella creazione del PDF", e);
        }

        return out.toByteArray();
    }

    /**
     * Aggiunge il watermark di sfondo se abilitato
     */
    private void addWatermarkIfEnabled(PdfWriter writer, CompanyInfo companyInfo) {
        if (companyInfo == null || !Boolean.TRUE.equals(companyInfo.getShowLogoInDocuments()) 
            || companyInfo.getWatermarkPath() == null) {
            return;
        }

        try {
            // Prova diversi percorsi per il watermark
            String watermarkPath = findFilePath(companyInfo.getWatermarkPath());
            if (watermarkPath == null) {
                log.warn("Watermark non trovato per il path: {}", companyInfo.getWatermarkPath());
                return;
            }

            Image watermark = Image.getInstance(watermarkPath);
            
            // Posiziona il watermark al centro della pagina con trasparenza
            float pageWidth = PageSize.A4.getWidth();
            float pageHeight = PageSize.A4.getHeight();
            
            // Scala il watermark mantenendo le proporzioni
            watermark.scaleToFit(pageWidth * 0.6f, pageHeight * 0.6f);
            
            // Centra il watermark
            watermark.setAbsolutePosition(
                (pageWidth - watermark.getScaledWidth()) / 2,
                (pageHeight - watermark.getScaledHeight()) / 2
            );
            
            // Imposta trasparenza del 70%
            com.lowagie.text.pdf.PdfGState gState = new com.lowagie.text.pdf.PdfGState();
            gState.setFillOpacity(0.3f); // 30% opacità = 70% trasparenza
            
            com.lowagie.text.pdf.PdfContentByte canvas = writer.getDirectContentUnder();
            canvas.saveState();
            canvas.setGState(gState);
            canvas.addImage(watermark);
            canvas.restoreState();
            
            log.info("Watermark aggiunto con successo");
            
        } catch (Exception e) {
            log.error("Errore nell'aggiunta del watermark: {}", e.getMessage());
        }
    }

    /**
     * Aggiunge l'header con logo e ragione sociale
     */
    private void addCompanyHeader(Document pdfDoc, CompanyInfo companyInfo) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100f);
        headerTable.setWidths(new float[]{1f, 3f});
        headerTable.setSpacingAfter(15f);
        
        // === LOGO (colonna sinistra) ===
        com.lowagie.text.pdf.PdfPCell logoCell = new com.lowagie.text.pdf.PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        
        if (companyInfo != null && Boolean.TRUE.equals(companyInfo.getShowLogoInDocuments()) 
            && companyInfo.hasLogo()) {
            try {
                String logoPath = findFilePath(companyInfo.getLogoPath());
                if (logoPath != null) {
                    Image logo = Image.getInstance(logoPath);
                    logo.scaleToFit(120f, 80f); // Logo più grande nell'header
                    logoCell.addElement(logo);
                    log.info("Logo aziendale aggiunto nell'header");
                } else {
                    log.warn("Logo non trovato per il path: {}", companyInfo.getLogoPath());
                    logoCell.addElement(new Paragraph("")); // Cella vuota
                }
            } catch (Exception e) {
                log.error("Errore nel caricamento del logo: {}", e.getMessage());
                logoCell.addElement(new Paragraph("")); // Cella vuota in caso di errore
            }
        } else {
            logoCell.addElement(new Paragraph("")); // Cella vuota se logo disabilitato
        }
        
        headerTable.addCell(logoCell);
        
        // === RAGIONE SOCIALE (colonna destra) ===
        Font companyNameFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24,
            companyInfo != null && companyInfo.getPrimaryColor() != null ? 
            Color.decode(companyInfo.getPrimaryColor()) : Color.decode("#667eea"));
        Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.GRAY);
        
        Paragraph companyNamePara = new Paragraph();
        
        if (companyInfo != null) {
            // Nome azienda principale
            companyNamePara.add(new Chunk(companyInfo.getDisplayName(), companyNameFont));
            companyNamePara.add(Chunk.NEWLINE);
            
            // Descrizione business se presente
            if (companyInfo.getBusinessDescription() != null && 
                !companyInfo.getBusinessDescription().trim().isEmpty()) {
                companyNamePara.add(new Chunk(companyInfo.getBusinessDescription(), subtitleFont));
            }
        } else {
            // Fallback
            companyNamePara.add(new Chunk("VEELY", companyNameFont));
            companyNamePara.add(Chunk.NEWLINE);
            companyNamePara.add(new Chunk("Fleet Management System", subtitleFont));
        }
        
        com.lowagie.text.pdf.PdfPCell companyCell = new com.lowagie.text.pdf.PdfPCell(companyNamePara);
        companyCell.setBorder(Rectangle.NO_BORDER);
        companyCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        companyCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        headerTable.addCell(companyCell);
        
        pdfDoc.add(headerTable);
    }

    /**
     * Aggiunge la linea separatrice colorata
     */
    private void addSeparatorLine(Document pdfDoc, CompanyInfo companyInfo) throws DocumentException {
        com.lowagie.text.pdf.PdfPTable line = new com.lowagie.text.pdf.PdfPTable(1);
        line.setWidthPercentage(100f);
        com.lowagie.text.pdf.PdfPCell lineCell = new com.lowagie.text.pdf.PdfPCell();
        lineCell.setBackgroundColor(companyInfo != null && companyInfo.getPrimaryColor() != null ? 
            Color.decode(companyInfo.getPrimaryColor()) : Color.decode("#667eea"));
        lineCell.setFixedHeight(3f);
        lineCell.setBorder(Rectangle.NO_BORDER);
        line.addCell(lineCell);
        line.setSpacingAfter(20f);
        pdfDoc.add(line);
    }

    /**
     * Aggiunge le informazioni principali del report
     */
    private void addReportInformation(Document pdfDoc, ExpenseReport report, CompanyInfo companyInfo) 
            throws DocumentException {
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14,
            companyInfo != null && companyInfo.getPrimaryColor() != null ? 
            Color.decode(companyInfo.getPrimaryColor()) : Color.decode("#667eea"));
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.decode("#555555"));
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.decode("#333333"));

        Paragraph sectionTitle = new Paragraph("INFORMAZIONI GENERALI", sectionFont);
        sectionTitle.setSpacingAfter(10f);
        pdfDoc.add(sectionTitle);

        PdfPTable infoTable = new PdfPTable(4);
        infoTable.setWidthPercentage(100f);
        infoTable.setWidths(new float[]{1f, 1.5f, 1f, 1.5f});
        infoTable.setSpacingAfter(20f);

        Color headerBg = Color.decode("#f8f9fa");
        Color cellBg = Color.WHITE;

        // Riga 1: Dipendente e Numero
        infoTable.addCell(createStyledCell("Dipendente:", labelFont, headerBg, true));
        infoTable.addCell(createStyledCell(
            report.getEmployee() != null ? report.getEmployee().getFirstName() + " " + report.getEmployee().getLastName() : "",
            valueFont, cellBg, false));
        infoTable.addCell(createStyledCell("Numero:", labelFont, headerBg, true));
        infoTable.addCell(createStyledCell(report.getExpenseReportNum() != null ? report.getExpenseReportNum() : "", 
            valueFont, cellBg, false));

        // Riga 2: Data e Scopo
        infoTable.addCell(createStyledCell("Data Creazione:", labelFont, headerBg, true));
        infoTable.addCell(createStyledCell(
            report.getCreationDate() != null ? 
            report.getCreationDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "",
            valueFont, cellBg, false));
        infoTable.addCell(createStyledCell("Scopo:", labelFont, headerBg, true));
        infoTable.addCell(createStyledCell(report.getPuorpose() != null ? report.getPuorpose() : "", 
            valueFont, cellBg, false));

        // Riga 3: Periodo
        infoTable.addCell(createStyledCell("Periodo Dal:", labelFont, headerBg, true));
        infoTable.addCell(createStyledCell(
            report.getStartDate() != null ? 
            report.getStartDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "",
            valueFont, cellBg, false));
        infoTable.addCell(createStyledCell("Periodo Al:", labelFont, headerBg, true));
        infoTable.addCell(createStyledCell(
            report.getEndDate() != null ? 
            report.getEndDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "",
            valueFont, cellBg, false));

        pdfDoc.add(infoTable);
    }

    /**
     * Aggiunge i dettagli delle spese
     */
    private void addExpenseDetails(Document pdfDoc, List<ExpenseItem> items, CompanyInfo companyInfo) 
            throws DocumentException {
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14,
            companyInfo != null && companyInfo.getPrimaryColor() != null ? 
            Color.decode(companyInfo.getPrimaryColor()) : Color.decode("#667eea"));

        Paragraph expenseTitle = new Paragraph("DETTAGLIO SPESE", sectionFont);
        expenseTitle.setSpacingAfter(10f);
        pdfDoc.add(expenseTitle);

        PdfPTable expenseTable = new PdfPTable(5);
        expenseTable.setWidthPercentage(100f);
        expenseTable.setWidths(new float[]{1f, 3f, 1.5f, 1.5f, 1.3f});

        // Header tabella
        Font tableHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        Color tableHeaderBg = companyInfo != null && companyInfo.getPrimaryColor() != null ? 
            Color.decode(companyInfo.getPrimaryColor()) : Color.decode("#667eea");

        expenseTable.addCell(createStyledCell("DATA", tableHeaderFont, tableHeaderBg, true));
        expenseTable.addCell(createStyledCell("DESCRIZIONE", tableHeaderFont, tableHeaderBg, true));
        expenseTable.addCell(createStyledCell("FORNITORE", tableHeaderFont, tableHeaderBg, true));
        expenseTable.addCell(createStyledCell("NOTE", tableHeaderFont, tableHeaderBg, true));
        expenseTable.addCell(createStyledCell("IMPORTO (€)", tableHeaderFont, tableHeaderBg, true));

        // Righe dati
        Font tableDataFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.decode("#333333"));
        Color alternateRowBg = Color.decode("#f8f9fa");
        
        for (int i = 0; i < items.size(); i++) {
            ExpenseItem item = items.get(i);
            Color rowBg = (i % 2 == 0) ? Color.WHITE : alternateRowBg;

            expenseTable.addCell(createStyledCell(
                item.getDate() != null ? 
                item.getDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "",
                tableDataFont, rowBg, false));

            String description = item.getDescription() != null ? item.getDescription() : "";
            if (description.length() > 40) {
                description = description.substring(0, 37) + "...";
            }
            expenseTable.addCell(createStyledCell(description, tableDataFont, rowBg, false));

            expenseTable.addCell(createStyledCell(
                item.getSupplier() != null ? item.getSupplier().getName() : "-",
                tableDataFont, rowBg, false));

            String note = item.getNote() != null ? item.getNote() : "-";
            if (note.length() > 30) {
                note = note.substring(0, 27) + "...";
            }
            expenseTable.addCell(createStyledCell(note, tableDataFont, rowBg, false));

            String amountStr = item.getAmount() != null ? 
                String.format("€ %.2f", item.getAmount()) : "€ 0,00";
            expenseTable.addCell(createStyledCell(amountStr, tableDataFont, rowBg, false));
        }

        expenseTable.setSpacingAfter(20f);
        pdfDoc.add(expenseTable);
    }

    /**
     * Aggiunge il riepilogo totali
     */
    private void addExpenseSummary(Document pdfDoc, ExpenseReport report, CompanyInfo companyInfo) 
            throws DocumentException {
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14,
            companyInfo != null && companyInfo.getPrimaryColor() != null ? 
            Color.decode(companyInfo.getPrimaryColor()) : Color.decode("#667eea"));

        Paragraph summaryTitle = new Paragraph("RIEPILOGO TOTALI", sectionFont);
        summaryTitle.setSpacingAfter(10f);
        pdfDoc.add(summaryTitle);

        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(60f);
        summaryTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        summaryTable.setWidths(new float[]{2f, 1f});

        Font summaryLabelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.decode("#555555"));
        Font summaryValueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.decode("#198754"));
        Color headerBg = Color.decode("#f8f9fa");
        Color cellBg = Color.WHITE;

        summaryTable.addCell(createStyledCell("Totale Nota Spese:", summaryLabelFont, headerBg, false));
        summaryTable.addCell(createStyledCell(
            String.format("€ %.2f", report.getExpenseReportTotal() != null ? report.getExpenseReportTotal() : BigDecimal.ZERO),
            summaryValueFont, cellBg, false));

        summaryTable.addCell(createStyledCell("Totale Rimborsabile:", summaryLabelFont, headerBg, false));
        summaryTable.addCell(createStyledCell(
            String.format("€ %.2f", report.getReimbursableTotal() != null ? report.getReimbursableTotal() : BigDecimal.ZERO),
            summaryValueFont, cellBg, false));

        summaryTable.addCell(createStyledCell("Totale Non Rimborsabile:", summaryLabelFont, headerBg, false));
        summaryTable.addCell(createStyledCell(
            String.format("€ %.2f", report.getNonReimbursableTotal() != null ? report.getNonReimbursableTotal() : BigDecimal.ZERO),
            summaryValueFont, cellBg, false));

        summaryTable.addCell(createStyledCell("Metodo di Pagamento:", summaryLabelFont, headerBg, false));
        summaryTable.addCell(createStyledCell(
            report.getPaymentMethodCode() != null ? report.getPaymentMethodCode().getDisplayName() : "-",
            summaryLabelFont, cellBg, false));

        summaryTable.setSpacingAfter(30f);
        pdfDoc.add(summaryTable);
    }

    /**
     * Aggiunge il footer aziendale con tutte le informazioni
     */
    private void addCompanyFooter(Document pdfDoc, ExpenseReport report, CompanyInfo companyInfo) 
            throws DocumentException {
        
        // Spazio per le firme
        PdfPTable signatureTable = new PdfPTable(2);
        signatureTable.setWidthPercentage(100f);
        signatureTable.setWidths(new float[]{1f, 1f});
        signatureTable.setSpacingAfter(20f);

        Font signatureFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.decode("#333333"));
        
        // Firma dipendente
        Paragraph employeeSignature = new Paragraph();
        employeeSignature.add(new Chunk("Il Dipendente", signatureFont));
        employeeSignature.add(Chunk.NEWLINE);
        employeeSignature.add(Chunk.NEWLINE);
        employeeSignature.add(new Chunk("_____________________", signatureFont));
        if (report.getEmployee() != null) {
            employeeSignature.add(Chunk.NEWLINE);
            employeeSignature.add(new Chunk(report.getEmployee().getFirstName() + " " + 
                report.getEmployee().getLastName(), signatureFont));
        }
        
        com.lowagie.text.pdf.PdfPCell empCell = new com.lowagie.text.pdf.PdfPCell(employeeSignature);
        empCell.setBorder(Rectangle.NO_BORDER);
        empCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        signatureTable.addCell(empCell);

        // Firma responsabile
        Paragraph managerSignature = new Paragraph();
        managerSignature.add(new Chunk("Il Responsabile", signatureFont));
        managerSignature.add(Chunk.NEWLINE);
        managerSignature.add(Chunk.NEWLINE);
        managerSignature.add(new Chunk("_____________________", signatureFont));
        if (companyInfo != null && companyInfo.getLegalRepresentative() != null) {
            managerSignature.add(Chunk.NEWLINE);
            managerSignature.add(new Chunk(companyInfo.getLegalRepresentative(), signatureFont));
        }
        
        com.lowagie.text.pdf.PdfPCell mgrCell = new com.lowagie.text.pdf.PdfPCell(managerSignature);
        mgrCell.setBorder(Rectangle.NO_BORDER);
        mgrCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        signatureTable.addCell(mgrCell);

        pdfDoc.add(signatureTable);

        // === FOOTER CON INFORMAZIONI AZIENDALI COMPLETE ===
        if (companyInfo != null) {
            addDetailedCompanyFooter(pdfDoc, companyInfo);
        }
    }

    /**
     * Aggiunge il footer dettagliato con tutte le informazioni aziendali
     */
    private void addDetailedCompanyFooter(Document pdfDoc, CompanyInfo companyInfo) throws DocumentException {
        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);
        Font footerBoldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.decode("#555555"));
        
        // Linea separatrice
        com.lowagie.text.pdf.PdfPTable separatorLine = new com.lowagie.text.pdf.PdfPTable(1);
        separatorLine.setWidthPercentage(100f);
        com.lowagie.text.pdf.PdfPCell sepCell = new com.lowagie.text.pdf.PdfPCell();
        sepCell.setBackgroundColor(Color.decode("#dee2e6"));
        sepCell.setFixedHeight(1f);
        sepCell.setBorder(Rectangle.NO_BORDER);
        separatorLine.addCell(sepCell);
        separatorLine.setSpacingAfter(10f);
        pdfDoc.add(separatorLine);

        PdfPTable footerTable = new PdfPTable(3);
        footerTable.setWidthPercentage(100f);
        footerTable.setWidths(new float[]{1f, 1f, 1f});

        // === COLONNA 1: INDIRIZZO ===
        Paragraph addressInfo = new Paragraph();
        if (Boolean.TRUE.equals(companyInfo.getShowAddressInDocuments())) {
            addressInfo.add(new Chunk("SEDE LEGALE", footerBoldFont));
            addressInfo.add(Chunk.NEWLINE);
            if (companyInfo.getFullLegalAddress() != null && !companyInfo.getFullLegalAddress().isEmpty()) {
                addressInfo.add(new Chunk(companyInfo.getFullLegalAddress(), footerFont));
            }
        }
        
        com.lowagie.text.pdf.PdfPCell addressCell = new com.lowagie.text.pdf.PdfPCell(addressInfo);
        addressCell.setBorder(Rectangle.NO_BORDER);
        addressCell.setVerticalAlignment(Element.ALIGN_TOP);
        footerTable.addCell(addressCell);

        // === COLONNA 2: CONTATTI ===
        Paragraph contactInfo = new Paragraph();
        if (Boolean.TRUE.equals(companyInfo.getShowContactsInDocuments())) {
            contactInfo.add(new Chunk("CONTATTI", footerBoldFont));
            contactInfo.add(Chunk.NEWLINE);
            if (companyInfo.getPrimaryPhone() != null) {
                contactInfo.add(new Chunk("Tel: " + companyInfo.getPrimaryPhone(), footerFont));
                contactInfo.add(Chunk.NEWLINE);
            }
            if (companyInfo.getPrimaryEmail() != null) {
                contactInfo.add(new Chunk("Email: " + companyInfo.getPrimaryEmail(), footerFont));
                contactInfo.add(Chunk.NEWLINE);
            }
            if (companyInfo.getPecEmail() != null) {
                contactInfo.add(new Chunk("PEC: " + companyInfo.getPecEmail(), footerFont));
                contactInfo.add(Chunk.NEWLINE);
            }
            if (companyInfo.getWebsite() != null) {
                contactInfo.add(new Chunk("Web: " + companyInfo.getWebsite(), footerFont));
            }
        }
        
        com.lowagie.text.pdf.PdfPCell contactCell = new com.lowagie.text.pdf.PdfPCell(contactInfo);
        contactCell.setBorder(Rectangle.NO_BORDER);
        contactCell.setVerticalAlignment(Element.ALIGN_TOP);
        footerTable.addCell(contactCell);

        // === COLONNA 3: DATI FISCALI E BANCARI ===
        Paragraph fiscalInfo = new Paragraph();
        if (Boolean.TRUE.equals(companyInfo.getShowTaxInfoInDocuments())) {
            fiscalInfo.add(new Chunk("DATI FISCALI", footerBoldFont));
            fiscalInfo.add(Chunk.NEWLINE);
            if (companyInfo.getVatNumber() != null) {
                fiscalInfo.add(new Chunk("P.IVA: " + companyInfo.getVatNumber(), footerFont));
                fiscalInfo.add(Chunk.NEWLINE);
            }
            if (companyInfo.getTaxCode() != null) {
                fiscalInfo.add(new Chunk("C.F.: " + companyInfo.getTaxCode(), footerFont));
                fiscalInfo.add(Chunk.NEWLINE);
            }
            if (companyInfo.getReaNumber() != null) {
                fiscalInfo.add(new Chunk("REA: " + companyInfo.getReaNumber(), footerFont));
                fiscalInfo.add(Chunk.NEWLINE);
            }
            if (companyInfo.getShareCapital() != null) {
                fiscalInfo.add(new Chunk("Cap. Soc.: " + companyInfo.getShareCapital(), footerFont));
            }
        }
        
        com.lowagie.text.pdf.PdfPCell fiscalCell = new com.lowagie.text.pdf.PdfPCell(fiscalInfo);
        fiscalCell.setBorder(Rectangle.NO_BORDER);
        fiscalCell.setVerticalAlignment(Element.ALIGN_TOP);
        footerTable.addCell(fiscalCell);

        pdfDoc.add(footerTable);
    }

    /**
     * Metodo helper per trovare il percorso corretto dei file
     */
    private String findFilePath(String relativePath) {
        if (relativePath == null) return null;
        
        String[] possiblePaths = {
            "uploads/" + relativePath,
            relativePath,
            Paths.get("uploads", relativePath).toString()
        };
        
        for (String path : possiblePaths) {
            Path fullPath = Paths.get(path);
            if (Files.exists(fullPath)) {
                return fullPath.toString();
            }
        }
        
        return null;
    }

    /**
     * Metodo helper per creare celle stilizzate
     */
    private com.lowagie.text.pdf.PdfPCell createStyledCell(String content, Font font, Color backgroundColor, boolean isBold) {
        Phrase phrase = new Phrase(content, font);
        com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(phrase);
        
        cell.setBackgroundColor(backgroundColor);
        cell.setPadding(6f);
        cell.setBorderWidth(0.5f);
        cell.setBorderColor(Color.decode("#dee2e6"));
        
        if (isBold) {
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        }
        
        return cell;
    }
}
