package com.veely.service;

import com.veely.entity.Employee;
import com.veely.entity.CompanyInfo;
import com.veely.model.EmploymentStatus;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service dedicato alla generazione del PDF con l'elenco dei dipendenti.
 * L'implementazione è ispirata a quella utilizzata per i rapporti di lavoro.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfEmployeeService {
	
	private final CompanyInfoService companyInfoService;

    /**
     * Esporta la lista dei dipendenti in formato PDF.
     */
    public byte[] exportStyledPdf(List<Employee> employees) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
        	Optional<CompanyInfo> companyInfoOpt = companyInfoService.getPrimaryCompanyInfoOptional();
            CompanyInfo companyInfo = companyInfoOpt.orElse(null);

            Document pdfDoc = new Document(PageSize.A4.rotate(), 12, 12, 15, 40);
            PdfWriter writer = PdfWriter.getInstance(pdfDoc, out);
            writer.setPageEvent(new EmployeeFooterEvent(companyInfo));
            pdfDoc.open();

            addHeader(pdfDoc, companyInfo);

            // Tabella principale
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100f);
            table.setWidths(new float[]{0.3f, 2.0f, 1.4f, 2.0f, 1.0f, 1.3f});
            table.setSpacingBefore(7f);

            addHeaderCell(table, "#");
            addHeaderCell(table, "DIPENDENTE");
            addHeaderCell(table, "COD. FISCALE");
            addHeaderCell(table, "EMAIL");
            addHeaderCell(table, "STATO");
            addHeaderCell(table, "CELLULARE");

            int idx = 1;
            for (Employee e : employees) {
                table.addCell(String.valueOf(idx++));
                table.addCell(safe(e.getFullName()));
                table.addCell(safe(e.getFiscalCode()));
                table.addCell(safe(e.getEmail()));
                String status = (e.getEmployments() != null && e.getEmployments().stream()
                        .anyMatch(emp -> emp.getStatus() == EmploymentStatus.ACTIVE)) ?
                        "ATTIVO" : "NON ATTIVO";
                table.addCell(status);
                String mobile = e.getMobile();
                if(mobile == null) {
                	mobile = e.getPhone();
                }
                table.addCell(mobile);
            }

            pdfDoc.add(table);
            pdfDoc.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            log.error("Errore nella generazione del PDF dei dipendenti", e);
            throw new RuntimeException("Errore nella creazione del PDF", e);
        }
    }

    private void addHeaderCell(PdfPTable table, String text) {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        PdfPCell cell = new PdfPCell(new Phrase(text, headerFont));
        cell.setBackgroundColor(Color.DARK_GRAY);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5f);
        table.addCell(cell);
    }

    private String safe(String val) {
        return val == null ? "" : val;
    }
    
    private void addHeader(Document pdfDoc, CompanyInfo companyInfo) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(3);
        headerTable.setWidthPercentage(100f);
        headerTable.setWidths(new float[]{1.5f, 3f, 1.5f});

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);

        Paragraph company = new Paragraph(extractCompanyName(companyInfo), titleFont);
        company.setAlignment(Element.ALIGN_LEFT);
        headerTable.addCell(createBorderlessCell(company));

        Paragraph title = new Paragraph("ELENCO DIPENDENTI", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        headerTable.addCell(createBorderlessCell(title));

        String dateStr = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        Paragraph date = new Paragraph(dateStr, smallFont);
        date.setAlignment(Element.ALIGN_RIGHT);
        headerTable.addCell(createBorderlessCell(date));

        pdfDoc.add(headerTable);
        addThinSeparatorLine(pdfDoc);
    }

    private String extractCompanyName(CompanyInfo companyInfo) {
        return (companyInfo != null && companyInfo.getCompanyName() != null)
                ? companyInfo.getCompanyName() : "-----";
    }

    private void addThinSeparatorLine(Document pdfDoc) throws DocumentException {
        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100f);
        line.setSpacingBefore(3f);
        line.setSpacingAfter(5f);

        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setFixedHeight(0.5f);
        cell.setBackgroundColor(Color.decode("#dee2e6"));
        line.addCell(cell);

        pdfDoc.add(line);
    }

    private PdfPCell createBorderlessCell(Paragraph content) {
        PdfPCell cell = new PdfPCell(content);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(2f);
        return cell;
    }

    private PdfPCell createFooterCell(String content, Font normalFont, Font boldFont) {
        Paragraph para = new Paragraph();
        String[] lines = content.split("\n");
        if (lines.length > 0 && !lines[0].isBlank()) {
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

    private String formatAddress(CompanyInfo companyInfo) {
        if (companyInfo == null) return "";
        String addr = companyInfo.getFullLegalAddress();
        return addr == null || addr.isEmpty() ? "" : "SEDE LEGALE\n" + addr;
    }

    private String formatContacts(CompanyInfo companyInfo) {
        if (companyInfo == null) return "";
        StringBuilder sb = new StringBuilder("CONTATTI\n");
        if (companyInfo.getPrimaryPhone() != null) {
            sb.append("Tel: ").append(companyInfo.getPrimaryPhone()).append("\n");
        }
        if (companyInfo.getPrimaryEmail() != null) {
            sb.append("Email: ").append(companyInfo.getPrimaryEmail()).append("\n");
        }
        return sb.toString().trim();
    }

    private String formatTaxInfo(CompanyInfo companyInfo) {
        if (companyInfo == null) return "";
        StringBuilder sb = new StringBuilder("DATI FISCALI\n");
        if (companyInfo.getVatNumber() != null) {
            sb.append("P.IVA: ").append(companyInfo.getVatNumber()).append("\n");
        }
        if (companyInfo.getTaxCode() != null) {
            sb.append("C.F.: ").append(companyInfo.getTaxCode());
        }
        return sb.toString().trim();
    }

    private class EmployeeFooterEvent extends PdfPageEventHelper {
        private final CompanyInfo companyInfo;

        private EmployeeFooterEvent(CompanyInfo companyInfo) {
            this.companyInfo = companyInfo;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            try {
                PdfPTable footerTable = new PdfPTable(3);
                footerTable.setWidthPercentage(100f);

                Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 7, Color.GRAY);
                Font footerBoldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, Color.DARK_GRAY);

                footerTable.addCell(createFooterCell(formatAddress(companyInfo), footerFont, footerBoldFont));
                footerTable.addCell(createFooterCell(formatContacts(companyInfo), footerFont, footerBoldFont));
                footerTable.addCell(createFooterCell(formatTaxInfo(companyInfo), footerFont, footerBoldFont));

                footerTable.setTotalWidth(document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin());
                float y = footerTable.getTotalHeight() + 28f; // 1cm dal bordo inferiore
                footerTable.writeSelectedRows(0, -1, document.leftMargin(), y, writer.getDirectContent());
            } catch (Exception ex) {
                // ignora errori nel footer
            }
        }
    }
}

