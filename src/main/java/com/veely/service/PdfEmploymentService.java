package com.veely.service;

// Import delle tue entità
import com.veely.entity.Employment;
import com.veely.entity.CompanyInfo;
import com.veely.model.EmploymentStatus;

// Import Spring
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// Import Java standard
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.time.temporal.ChronoUnit;

// Import PDF - NESSUN CONFLITTO qui!
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import java.awt.Color;

/**
 * Service dedicato alla generazione di PDF per i rapporti di lavoro.
 * Separato dal main service per evitare conflitti di import.
 * 
 * @author Senior Java Developer
 * @version 2.0 - Ottimizzato per spazio e integrazione CompanyInfo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfEmploymentService {

    private final CompanyInfoService companyInfoService;

    // === COSTANTI DI LAYOUT ===
    private static final Color SINGOL_PRIMARY_COLOR = Color.decode("#2c5f5f");
    private static final Color SUCCESS_COLOR = Color.decode("#198754");
    private static final Color DANGER_COLOR = Color.decode("#dc3545");
    private static final Color WARNING_COLOR = Color.decode("#ffc107");
    private static final Color LIGHT_GRAY = Color.decode("#f8f9fa");
    private static final Color BORDER_COLOR = Color.decode("#dee2e6");

    /**
     * Esporta PDF con header completo (include riepilogo statistiche)
     */
    public byte[] exportStyledPdf(List<Employment> employments) throws IOException {
        return exportStyledPdf(employments, true);
    }

    /**
     * Esporta PDF dei rapporti di lavoro con design SINGOL ottimizzato
     * 
     * @param employments Lista dei rapporti di lavoro
     * @param includeHeader Se includere il riepilogo statistiche
     * @return PDF generato come array di byte
     */
    public byte[] exportStyledPdf(List<Employment> employments, boolean includeHeader) throws IOException {
        Optional<CompanyInfo> companyInfoOpt = companyInfoService.getPrimaryCompanyInfoOptional();
        CompanyInfo companyInfo = companyInfoOpt.orElse(null);

        try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            // Formato orizzontale con margini ultra-ridotti
            Document pdfDoc = new Document(PageSize.A4.rotate(), 12, 12, 15, 15);
            
            PdfWriter writer = PdfWriter.getInstance(pdfDoc, out);
            pdfDoc.open();

            // === WATERMARK OPZIONALE ===
            addWatermarkIfAvailable(writer, companyInfo);

            // === HEADER ULTRA-COMPATTO ===
            addUltraCompactHeader(pdfDoc, companyInfo);

            // === RIEPILOGO STATISTICHE (OPZIONALE) ===
            if (includeHeader && !employments.isEmpty()) {
                addCompactSummary(pdfDoc, employments);
            }

            // === TABELLA PRINCIPALE ===
            addOptimizedEmploymentsTable(pdfDoc, employments);

            // === FOOTER MINIMALISTA ===
            addMinimalistFooter(pdfDoc, companyInfo);

            pdfDoc.close();
            return out.toByteArray();
            
        } catch (DocumentException e) {
            log.error("Errore nella generazione del PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Errore nella creazione del PDF", e);
        }
    }

    /**
     * Header ultra-compatto che occupa max 1/5 della pagina
     */
    private void addUltraCompactHeader(Document pdfDoc, CompanyInfo companyInfo) throws DocumentException {
        // Header su singola riga - max 20pt di altezza
        PdfPTable headerTable = new PdfPTable(3);
        headerTable.setWidthPercentage(100f);
        headerTable.setWidths(new float[]{1.5f, 3f, 1.5f});
        
        // Nessun spacing aggiuntivo per minimizzare l'altezza
        Font compactFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, SINGOL_PRIMARY_COLOR);
        Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);

        // === LOGO/NOME AZIENDA ===
        String companyName = extractCompanyName(companyInfo);
        Paragraph logoText = new Paragraph(companyName, compactFont);
        logoText.setAlignment(Element.ALIGN_LEFT);
        
        PdfPCell logoCell = createBorderlessCell(logoText);
        headerTable.addCell(logoCell);

        // === TITOLO CENTRALE ===
        Paragraph title = new Paragraph("ELENCO RAPPORTI DI LAVORO", compactFont);
        title.setAlignment(Element.ALIGN_CENTER);
        
        PdfPCell titleCell = createBorderlessCell(title);
        headerTable.addCell(titleCell);

        // === DATA ===
        String dateStr = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        Paragraph dateText = new Paragraph(dateStr, smallFont);
        dateText.setAlignment(Element.ALIGN_RIGHT);
        
        PdfPCell dateCell = createBorderlessCell(dateText);
        headerTable.addCell(dateCell);

        pdfDoc.add(headerTable);

        // Linea separatrice sottilissima
        addThinSeparatorLine(pdfDoc);
    }

    /**
     * Riepilogo statistiche ultra-compatto
     */
    private void addCompactSummary(Document pdfDoc, List<Employment> employments) throws DocumentException {
        long totalEmployments = employments.size();
        long activeEmployments = employments.stream()
            .filter(e -> e.getStatus() == EmploymentStatus.ACTIVE)
            .count();
        long terminatedEmployments = totalEmployments - activeEmployments;

        // Tabella summary in una sola riga compatta
        PdfPTable summaryTable = new PdfPTable(3);
        summaryTable.setWidthPercentage(60f);
        summaryTable.setHorizontalAlignment(Element.ALIGN_CENTER);
        summaryTable.setSpacingAfter(8f);

        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.DARK_GRAY);
        
        summaryTable.addCell(createCompactSummaryCell("Totale: " + totalEmployments, labelFont));
        summaryTable.addCell(createCompactSummaryCell("Attivi: " + activeEmployments, 
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, SUCCESS_COLOR)));
        summaryTable.addCell(createCompactSummaryCell("Terminati: " + terminatedEmployments, 
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, DANGER_COLOR)));

        pdfDoc.add(summaryTable);
    }

    /**
     * Tabella ottimizzata con numerazione progressiva e matricola
     */
    private void addOptimizedEmploymentsTable(Document pdfDoc, List<Employment> employments) throws DocumentException {
    	// Tabella con 9 colonne: #, Matr, Dipendente, Mansione, Qualifica, Commessa, Data Inizio, Data Fine, Stato
        PdfPTable table = new PdfPTable(9);
        table.setWidthPercentage(100f);
        table.setWidths(new float[]{0.4f, 0.4f, 2.0f, 1.7f, 1.1f, 1.4f, 1.0f, 1.0f, 0.8f});

        // Header tabella
        addTableHeaders(table);

        // Righe dati
        addTableRows(table, employments);

        pdfDoc.add(table);
    }

    /**
     * Aggiunge le intestazioni della tabella
     */
    private void addTableHeaders(PdfPTable table) {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        
        String[] headers = {"#", "MATR", "DIPENDENTE", "MANSIONE", "QUALIFICA", "COMMESSA", "INIZIO", "FINE", "STATO"};
        
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(SINGOL_PRIMARY_COLOR);
            cell.setPadding(6f);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setBorderColor(Color.WHITE);
            table.addCell(cell);
        }
    }

    /**
     * Aggiunge le righe dati alla tabella
     */
    private void addTableRows(PdfPTable table, List<Employment> employments) {
        Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY);
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.DARK_GRAY);
        
        for (int i = 0; i < employments.size(); i++) {
            Employment emp = employments.get(i);
            Color rowBg = (i % 2 == 0) ? Color.WHITE : LIGHT_GRAY;

            // Numerazione progressiva
            table.addCell(createDataCell(String.valueOf(i + 1), dataFont, rowBg, Element.ALIGN_CENTER));

            // Matricola
            String matricola = emp.getMatricola() != null ? emp.getMatricola() : "-";
            table.addCell(createDataCell(matricola, dataFont, rowBg, Element.ALIGN_CENTER));

            // Dipendente
            String dipendente = formatEmployeeName(emp);
            table.addCell(createDataCell(dipendente, boldFont, rowBg, Element.ALIGN_LEFT));

            //Mansione
            String posizione = formatPosition(emp);
            table.addCell(createDataCell(posizione, dataFont, rowBg, Element.ALIGN_LEFT));

            // Qualifica
            String mansione = formatMansione(emp);
            table.addCell(createDataCell(mansione, boldFont, rowBg, Element.ALIGN_LEFT));
            
         // Commessa attuale
            String commessa = emp.getCurrentProjectName() != null ? emp.getCurrentProjectName() : "-";
            table.addCell(createDataCell(commessa, dataFont, rowBg, Element.ALIGN_LEFT));
            
            // Data inizio
            String dataInizio = emp.getStartDate() != null ? 
                emp.getStartDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "-";
            table.addCell(createDataCell(dataInizio, dataFont, rowBg, Element.ALIGN_CENTER));

            // Data fine
            String dataFine = emp.getEndDate() != null ?
                emp.getEndDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "Indet.";
            table.addCell(createDataCell(dataFine, dataFont, rowBg, Element.ALIGN_CENTER));

            // Stato
            String statusText = emp.getStatus().getDisplayName();
            Color statusColor = emp.getStatus() == EmploymentStatus.ACTIVE ? SUCCESS_COLOR : Color.GRAY;
            
            if (emp.getContractType() != null
                    && emp.getContractType() != com.veely.model.ContractType.PERMANENT
                    && emp.getEndDate() != null) {
                    LocalDate endDate = emp.getEndDate();
                    LocalDate today = LocalDate.now();

                    if (today.isAfter(endDate)) {
                        statusText = "Scaduto";
                        statusColor = DANGER_COLOR;
                    } else {
                        long daysUntilEnd = ChronoUnit.DAYS.between(today, endDate);
                        if (daysUntilEnd < 30) {
                            statusText = "In scadenza";
                            statusColor = WARNING_COLOR;
                        }
                    }
                }
            
            Font statusFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, statusColor);
            table.addCell(createDataCell(statusText, statusFont, rowBg, Element.ALIGN_CENTER));
        }
    }

    /**
     * Footer minimalista con dati aziendali essenziali
     */
    private void addMinimalistFooter(Document pdfDoc, CompanyInfo companyInfo) throws DocumentException {
        addThinSeparatorLine(pdfDoc);
        
        PdfPTable footerTable = new PdfPTable(3);
        footerTable.setWidthPercentage(100f);
        footerTable.setSpacingBefore(8f);

        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 7, Color.GRAY);
        Font footerBoldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, SINGOL_PRIMARY_COLOR);

        // Indirizzo
        footerTable.addCell(createFooterCell(formatAddress(companyInfo), footerFont, footerBoldFont));
        
        // Contatti
        footerTable.addCell(createFooterCell(formatContacts(companyInfo), footerFont, footerBoldFont));
        
        // Dati fiscali
        footerTable.addCell(createFooterCell(formatTaxInfo(companyInfo), footerFont, footerBoldFont));

        pdfDoc.add(footerTable);
    }

    // === METODI HELPER ===

    /**
     * Aggiunge watermark se disponibile
     */
    private void addWatermarkIfAvailable(PdfWriter writer, CompanyInfo companyInfo) {
        try {
            String watermarkPath = findWatermarkPath(companyInfo);
            if (watermarkPath == null) {
                log.debug("Nessun watermark configurato");
                return;
            }

            Image watermark = Image.getInstance(watermarkPath);
            float pageWidth = PageSize.A4.rotate().getWidth();
            float pageHeight = PageSize.A4.rotate().getHeight();
            
            watermark.scaleToFit(pageWidth * 0.7f, pageHeight * 0.7f);
            watermark.setAbsolutePosition(
                (pageWidth - watermark.getScaledWidth()) / 2,
                (pageHeight - watermark.getScaledHeight()) / 2
            );
            
            PdfGState gState = new PdfGState();
            gState.setFillOpacity(0.1f);
            
            PdfContentByte canvas = writer.getDirectContentUnder();
            canvas.saveState();
            canvas.setGState(gState);
            canvas.addImage(watermark);
            canvas.restoreState();
            
        } catch (Exception e) {
            log.warn("Impossibile aggiungere watermark: {}", e.getMessage());
        }
    }

    private String findWatermarkPath(CompanyInfo companyInfo) {
        if (companyInfo == null) return null;
        
        String[] possiblePaths = {
            companyInfo.getWatermarkPath(),
            companyInfo.getLogoPath(),
            "singol-watermark.png",
            "singol-logo.png"
        };
        
        for (String path : possiblePaths) {
            if (path != null && Files.exists(Paths.get("uploads", path))) {
                return Paths.get("uploads", path).toString();
            }
        }
        return null;
    }

    private String extractCompanyName(CompanyInfo companyInfo) {
        if (companyInfo != null && companyInfo.getCompanyName() != null) {
            return companyInfo.getCompanyName();
        }
        return "-----";
    }

    private String formatEmployeeName(Employment emp) {
        if (emp.getEmployee() == null) return "-";
        return emp.getEmployee().getFirstName() + " " + emp.getEmployee().getLastName();
    }

    
    private String formatMansione(Employment emp) {
    	String title = emp.getJobTitle();
        if (title == null || title.isBlank()) {
            return "-";
        }
        return title.trim();
    }
    
    private String formatPosition(Employment emp) {
       
    	String role = "";
        if (emp.getJobRole() != null && !emp.getJobRole().isBlank()) {
            role = emp.getJobRole().trim();
        }

        String level = emp.getContractLevel();
        if (level != null) {
            level = level.trim();
        }

        if (level != null && !level.isEmpty() && !level.toLowerCase(Locale.ITALIAN).startsWith("liv")) {
            level = "Liv. " + level;
        }

        if (!role.isEmpty() && level != null && !level.isEmpty()) {
            return role + " - " + level;
        }
        if (!role.isEmpty()) {
            return role;
        }
        if (level != null && !level.isEmpty()) {
            return level;
        }
        return "-";
    }

    private String formatAddress(CompanyInfo companyInfo) {
        if (companyInfo == null) return "Indirizzo non disponibile";
        
        String address = companyInfo.getFullLegalAddress();
        return address.isEmpty() ? "Sede legale non specificata" : address;
    }

    private String formatContacts(CompanyInfo companyInfo) {
        if (companyInfo == null) return "Contatti non disponibili";
        
        StringBuilder contacts = new StringBuilder();
        if (companyInfo.getPrimaryPhone() != null) {
            contacts.append("Tel: ").append(companyInfo.getPrimaryPhone()).append("\n");
        }
        if (companyInfo.getPrimaryEmail() != null) {
            contacts.append("Email: ").append(companyInfo.getPrimaryEmail());
        }
        
        return contacts.toString().isEmpty() ? "Contatti non specificati" : contacts.toString();
    }

    private String formatTaxInfo(CompanyInfo companyInfo) {
        if (companyInfo == null) return "Dati fiscali non disponibili";
        
        StringBuilder taxInfo = new StringBuilder();
        if (companyInfo.getVatNumber() != null) {
            taxInfo.append("P.IVA: ").append(companyInfo.getVatNumber()).append("\n");
        }
        if (companyInfo.getTaxCode() != null) {
            taxInfo.append("C.F.: ").append(companyInfo.getTaxCode());
        }
        
        return taxInfo.toString().isEmpty() ? "Dati fiscali non specificati" : taxInfo.toString();
    }

    private void addThinSeparatorLine(Document pdfDoc) throws DocumentException {
        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100f);
        line.setSpacingBefore(3f);
        line.setSpacingAfter(5f);
        
        PdfPCell lineCell = new PdfPCell();
        lineCell.setBackgroundColor(SINGOL_PRIMARY_COLOR);
        lineCell.setFixedHeight(0.5f);
        lineCell.setBorder(Rectangle.NO_BORDER);
        line.addCell(lineCell);
        
        pdfDoc.add(line);
    }

    private PdfPCell createBorderlessCell(Paragraph content) {
        PdfPCell cell = new PdfPCell(content);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(2f);
        return cell;
    }

    private PdfPCell createCompactSummaryCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(3f);
        return cell;
    }

    private PdfPCell createDataCell(String content, Font font, Color backgroundColor, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(content, font));
        cell.setBackgroundColor(backgroundColor);
        cell.setBorderColor(BORDER_COLOR);
        cell.setBorderWidth(0.5f);
        cell.setPadding(4f);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private PdfPCell createFooterCell(String content, Font normalFont, Font boldFont) {
        Paragraph para = new Paragraph();
        
        // Prima riga in grassetto
        String[] lines = content.split("\n");
        if (lines.length > 0) {
            para.add(new Chunk(lines[0], boldFont));
            for (int i = 1; i < lines.length; i++) {
                para.add(Chunk.NEWLINE);
                para.add(new Chunk(lines[i], normalFont));
            }
        }
        
        PdfPCell cell = new PdfPCell(para);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setVerticalAlignment(Element.ALIGN_TOP);
        cell.setPadding(3f);
        return cell;
    }
}