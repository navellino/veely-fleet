package com.veely.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.veely.entity.CompanyInfo;
import com.veely.entity.Payslip;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfPayslipDispatchReportService {

    private static final DateTimeFormatter MONTH_HEADER_FORMATTER =
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ITALY);
    private static final DateTimeFormatter MONTH_CELL_FORMATTER =
            DateTimeFormatter.ofPattern("MM/yyyy");
    private static final DateTimeFormatter SENT_AT_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final CompanyInfoService companyInfoService;

    public byte[] exportMonthlyReport(YearMonth referenceMonth, List<Payslip> payslips) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Optional<CompanyInfo> companyInfoOpt = companyInfoService.getPrimaryCompanyInfoOptional();
            CompanyInfo companyInfo = companyInfoOpt.orElse(null);

            Document document = new Document(PageSize.A4.rotate(), 12, 12, 15, 40);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new FooterEvent(companyInfo));
            document.open();

            addHeader(document, companyInfo, referenceMonth);
            if (payslips == null || payslips.isEmpty()) {
                Paragraph emptyMessage = new Paragraph(
                        "Nessun cedolino inviato per il mese selezionato.",
                        FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY));
                emptyMessage.setSpacingBefore(10f);
                document.add(emptyMessage);
            } else {
                PdfPTable table = new PdfPTable(6);
                table.setWidthPercentage(100f);
                table.setWidths(new float[]{0.5f, 2.5f, 1.5f, 1.0f, 1.5f, 2.5f});
                table.setSpacingBefore(10f);

                addHeaderCell(table, "#");
                addHeaderCell(table, "DIPENDENTE");
                addHeaderCell(table, "COD. FISCALE");
                addHeaderCell(table, "MESE RIF.");
                addHeaderCell(table, "DATA INVIO");
                addHeaderCell(table, "EMAIL INOLTRO");

                int index = 1;
                for (Payslip payslip : payslips) {
                    table.addCell(createIndexCell(index++));
                    table.addCell(createDataCell(safe(payslip.getDisplayName())));
                    table.addCell(createDataCell(safe(payslip.getFiscalCode())));
                    table.addCell(createDataCell(formatMonth(referenceMonth)));
                    table.addCell(createDataCell(formatSentAt(payslip)));
                    table.addCell(createDataCell(resolveEmail(payslip)));
                }

                document.add(table);
            }

            document.close();
            return out.toByteArray();
        } catch (DocumentException ex) {
            log.error("Errore durante la generazione del PDF con l'elenco dei cedolini inviati", ex);
            throw new IOException("Errore nella generazione del PDF", ex);
        }
    }

    private void addHeader(Document document, CompanyInfo companyInfo, YearMonth referenceMonth) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(3);
        headerTable.setWidthPercentage(100f);
        headerTable.setWidths(new float[]{1.5f, 3f, 1.5f});

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);

        Paragraph company = new Paragraph(extractCompanyName(companyInfo), titleFont);
        company.setAlignment(Element.ALIGN_LEFT);
        headerTable.addCell(createBorderlessCell(company));

        String monthLabel = referenceMonth != null
                ? referenceMonth.format(MONTH_HEADER_FORMATTER)
                : "";
        Paragraph title = new Paragraph("CEDOLINI INVIATI – " + monthLabel.toUpperCase(Locale.ITALIAN), titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        headerTable.addCell(createBorderlessCell(title));

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        Paragraph date = new Paragraph(dateStr, smallFont);
        date.setAlignment(Element.ALIGN_RIGHT);
        headerTable.addCell(createBorderlessCell(date));

        document.add(headerTable);
        addSeparator(document);
    }

    private void addSeparator(Document document) throws DocumentException {
        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100f);
        line.setSpacingBefore(3f);
        line.setSpacingAfter(7f);

        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setFixedHeight(0.5f);
        cell.setBackgroundColor(Color.decode("#dee2e6"));
        line.addCell(cell);

        document.add(line);
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

    private PdfPCell createIndexCell(int index) {
        PdfPCell cell = new PdfPCell(new Phrase(String.valueOf(index),
                FontFactory.getFont(FontFactory.HELVETICA, 9)));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4f);
        return cell;
    }

    private PdfPCell createDataCell(String content) {
        PdfPCell cell = new PdfPCell(new Phrase(content, FontFactory.getFont(FontFactory.HELVETICA, 9)));
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4f);
        return cell;
    }

    private PdfPCell createBorderlessCell(Paragraph content) {
        PdfPCell cell = new PdfPCell(content);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(2f);
        return cell;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String formatMonth(YearMonth referenceMonth) {
        if (referenceMonth == null) {
            return "";
        }
        return referenceMonth.format(MONTH_CELL_FORMATTER);
    }

    private String formatSentAt(Payslip payslip) {
        if (payslip == null || payslip.getSentAt() == null) {
            return "";
        }
        return payslip.getSentAt().format(SENT_AT_FORMATTER);
    }

    private String resolveEmail(Payslip payslip) {
        if (payslip == null) {
            return "";
        }
        if (payslip.getSentTo() != null) {
            return payslip.getSentTo();
        }
        if (payslip.getEmployee() != null) {
            return safe(payslip.getEmployee().getEmail());
        }
        return "";
    }

    private String extractCompanyName(CompanyInfo companyInfo) {
        if (companyInfo == null || companyInfo.getCompanyName() == null) {
            return "-----";
        }
        return companyInfo.getCompanyName();
    }

    private class FooterEvent extends PdfPageEventHelper {
        private final CompanyInfo companyInfo;

        private FooterEvent(CompanyInfo companyInfo) {
            this.companyInfo = companyInfo;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            try {
                PdfPTable footerTable = new PdfPTable(3);
                footerTable.setTotalWidth(document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin());
                footerTable.setWidths(new float[]{1.5f, 1.5f, 1.5f});

                Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 7);
                Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7);

                footerTable.addCell(createFooterCell(formatAddress(companyInfo), normalFont, boldFont));
                footerTable.addCell(createFooterCell(formatContacts(companyInfo), normalFont, boldFont));
                footerTable.addCell(createFooterCell(formatTaxInfo(companyInfo), normalFont, boldFont));

                footerTable.writeSelectedRows(0, -1, document.leftMargin(), document.bottomMargin() - 5,
                        writer.getDirectContent());
            } catch (DocumentException ex) {
                log.warn("Impossibile disegnare il footer del PDF", ex);
            }
        }
    }

    private PdfPCell createFooterCell(String content, Font normalFont, Font boldFont) {
        Paragraph para = new Paragraph();
        if (content != null && !content.isBlank()) {
            String[] lines = content.split("\n");
            if (lines.length > 0) {
                para.add(new Chunk(lines[0], boldFont));
                for (int i = 1; i < lines.length; i++) {
                    para.add(Chunk.NEWLINE);
                    para.add(new Chunk(lines[i], normalFont));
                }
            }
        }
        PdfPCell cell = new PdfPCell(para);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setVerticalAlignment(Element.ALIGN_TOP);
        cell.setPadding(3f);
        return cell;
    }

    private String formatAddress(CompanyInfo companyInfo) {
        if (companyInfo == null) {
            return "";
        }
        String address = companyInfo.getFullLegalAddress();
        if (address == null || address.isBlank()) {
            return "";
        }
        return "SEDE LEGALE\n" + address;
    }

    private String formatContacts(CompanyInfo companyInfo) {
        if (companyInfo == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder("CONTATTI\n");
        if (companyInfo.getPrimaryPhone() != null && !companyInfo.getPrimaryPhone().isBlank()) {
            sb.append("Tel: ").append(companyInfo.getPrimaryPhone()).append('\n');
        }
        if (companyInfo.getPrimaryEmail() != null && !companyInfo.getPrimaryEmail().isBlank()) {
            sb.append("Email: ").append(companyInfo.getPrimaryEmail()).append('\n');
        }
        return sb.toString().trim();
    }

    private String formatTaxInfo(CompanyInfo companyInfo) {
        if (companyInfo == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder("DATI FISCALI\n");
        if (companyInfo.getVatNumber() != null && !companyInfo.getVatNumber().isBlank()) {
            sb.append("P.IVA: ").append(companyInfo.getVatNumber()).append('\n');
        }
        if (companyInfo.getTaxCode() != null && !companyInfo.getTaxCode().isBlank()) {
            sb.append("C.F.: ").append(companyInfo.getTaxCode());
        }
        return sb.toString().trim();
    }
}
