package com.veely.controller;

import com.veely.dto.payslip.PayslipSendResult;
import com.veely.dto.payslip.PayslipUploadResult;
import com.veely.entity.Payslip;
import com.veely.entity.UniqueCertification;
import com.veely.service.PayslipEmailService;
import com.veely.service.PayslipService;
import com.veely.service.PdfPayslipDispatchReportService;
import com.veely.service.UniqueCertificationEmailService;
import com.veely.service.UniqueCertificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Slf4j
@Controller
@RequestMapping("/payslips")
@RequiredArgsConstructor
public class PayslipController {

    // Servizi
    private final PayslipService payslipService;
    private final PayslipEmailService payslipEmailService;
    private final PdfPayslipDispatchReportService pdfPayslipDispatchReportService;
    private final UniqueCertificationService uniqueCertificationService;
    private final UniqueCertificationEmailService uniqueCertificationEmailService;

    // Formattatori per date
    private static final DateTimeFormatter MONTH_INPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter MONTH_DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ITALY);
    private static final DateTimeFormatter YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter MONTH_FILE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");


    @GetMapping
    public String index(@RequestParam(value = "month", required = false) String month,
                        @RequestParam(value = "year", required = false) String year,
                        Model model) {
        YearMonth selectedMonth = resolveMonth(month);
        List<Payslip> payslips = payslipService.findByReferenceMonth(selectedMonth);

        Year selectedYear = resolveYear(year);
        List<UniqueCertification> certifications = uniqueCertificationService.findByReferenceYear(selectedYear);

        // Attributi per la sezione Cedolini
        model.addAttribute("payslips", payslips);
        model.addAttribute("selectedMonth", selectedMonth);
        model.addAttribute("selectedMonthValue", selectedMonth.format(MONTH_INPUT_FORMATTER));
        model.addAttribute("availableMonths", payslipService.getAvailableMonths());

        // Attributi per la sezione Certificazioni Uniche
        List<Year> availableYears = new ArrayList<>(uniqueCertificationService.getAvailableYears());
        if (availableYears.stream().noneMatch(yearOpt -> yearOpt.equals(selectedYear))) {
            availableYears.add(selectedYear);
        }
        availableYears.sort(Comparator.reverseOrder());

        model.addAttribute("certifications", certifications);
        model.addAttribute("selectedYear", selectedYear);
        model.addAttribute("selectedYearValue", selectedYear.format(YEAR_FORMATTER));
        model.addAttribute("availableYears", availableYears);
        
        // Contenuti predefiniti per email Cedolini
        String monthLabel = selectedMonth.format(MONTH_DISPLAY_FORMATTER);
        String defaultSubject = "Cedolino paga – " + monthLabel;
        String defaultBody = "Gentile collaboratore,\n"
                + "in allegato Le trasmettiamo il Suo cedolino paga relativo al mese di " + monthLabel + ".\n"
                + "La invitiamo a conservarne copia per i Suoi archivi personali.\n"
                + "Per qualsiasi chiarimento o segnalazione può rivolgersi all’Ufficio Amministrazione del Personale all’indirizzo risorseumane@sincolsrl.it.\n\n"
                + "Cordiali saluti,\n"
                + "Sincol S.r.l. – Ufficio Risorse Umane";

        String lastSubject = (String) model.asMap().get("lastSubject");
        if (!StringUtils.hasText(lastSubject)) {
            model.addAttribute("lastSubject", defaultSubject);
        }
        String lastBody = (String) model.asMap().get("lastBody");
        if (!StringUtils.hasText(lastBody)) {
            model.addAttribute("lastBody", defaultBody);
        }

        // Contenuti predefiniti per email Certificazioni Uniche
        String yearLabel = selectedYear.format(YEAR_FORMATTER);
        String defaultCertificationSubject = "Certificazione Unica – " + yearLabel;
        String defaultCertificationBody = "Gentile collaboratore,\n"
                + "in allegato trova la Sua Certificazione Unica relativa all'anno " + yearLabel + ".\n"
                + "La invitiamo a conservarne copia per i Suoi archivi personali.\n"
                + "Per qualsiasi chiarimento può contattarci all’indirizzo risorseumane@sincolsrl.it.\n\n"
                + "Cordiali saluti,\n"
                + "Sincol S.r.l. – Ufficio Risorse Umane";

        String lastCertificationSubject = (String) model.asMap().get("lastCertificationSubject");
        if (!StringUtils.hasText(lastCertificationSubject)) {
            model.addAttribute("lastCertificationSubject", defaultCertificationSubject);
        }
        String lastCertificationBody = (String) model.asMap().get("lastCertificationBody");
        if (!StringUtils.hasText(lastCertificationBody)) {
            model.addAttribute("lastCertificationBody", defaultCertificationBody);
        }

        return "payslips/index";
    }

    // --- SEZIONE GESTIONE CEDOLINI ---

    @PostMapping("/upload")
    public String upload(@RequestParam("referenceMonth") String referenceMonth,
                         @RequestParam(value = "files", required = false) MultipartFile[] files,
                         @RequestParam(value = "currentYear", required = false) String currentYear,
                         RedirectAttributes redirectAttributes) {
        YearMonth month = resolveMonth(referenceMonth);
        PayslipUploadResult result = payslipService.uploadPayslips(month, files);
        redirectAttributes.addFlashAttribute("uploadResult", result);
        redirectAttributes.addFlashAttribute("selectedMonth", month);
        Year year = resolveYear(currentYear);
        return "redirect:/payslips?month=" + month.format(MONTH_INPUT_FORMATTER)
                + "&year=" + year.format(YEAR_FORMATTER);
    }

    @PostMapping("/send")
    public String send(@RequestParam("referenceMonth") String referenceMonth,
                       @RequestParam(value = "payslipIds", required = false) List<Long> payslipIds,
                       @RequestParam(value = "subject", required = false) String subject,
                       @RequestParam(value = "body", required = false) String body,
                       @RequestParam(value = "currentYear", required = false) String currentYear,
                       RedirectAttributes redirectAttributes) {
        YearMonth month = resolveMonth(referenceMonth);
        String sanitizedSubject = subject != null ? subject.trim() : "";
        String sanitizedBody = body != null ? body.trim() : "";
        PayslipSendResult result = payslipEmailService.sendPayslips(
                payslipIds == null ? List.of() : payslipIds,
                sanitizedSubject,
                sanitizedBody);
        redirectAttributes.addFlashAttribute("sendResult", result);
        redirectAttributes.addFlashAttribute("lastSubject", sanitizedSubject);
        redirectAttributes.addFlashAttribute("lastBody", sanitizedBody);
        Year year = resolveYear(currentYear);
        return "redirect:/payslips?month=" + month.format(MONTH_INPUT_FORMATTER)
                + "&year=" + year.format(YEAR_FORMATTER);
    }
    
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportMonthlyReport(@RequestParam("month") String month) throws IOException {
        YearMonth referenceMonth = resolveMonth(month);
        List<Payslip> payslips = payslipService.findSentByReferenceMonth(referenceMonth);
        byte[] pdf = pdfPayslipDispatchReportService.exportMonthlyReport(referenceMonth, payslips);

        String filename = String.format("cedolini-inviati-%s.pdf", referenceMonth.format(MONTH_FILE_FORMATTER));
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(filename)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }


    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        Resource resource = payslipService.loadPayslipFile(id);
        String filename = resource.getFilename();
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(filename != null ? filename : "cedolino.pdf")
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @RequestParam("referenceMonth") String referenceMonth,
                         @RequestParam(value = "currentYear", required = false) String currentYear,
                         RedirectAttributes redirectAttributes) {
        YearMonth month = resolveMonth(referenceMonth);
        payslipService.deletePayslip(id);
        redirectAttributes.addFlashAttribute("deleteMessage", "Cedolino eliminato con successo");
        Year year = resolveYear(currentYear);
        return "redirect:/payslips?month=" + month.format(MONTH_INPUT_FORMATTER)
                + "&year=" + year.format(YEAR_FORMATTER);
    }

    @PostMapping("/delete-selected")
    public String deleteSelected(@RequestParam("referenceMonth") String referenceMonth,
                                 @RequestParam(value = "payslipIds", required = false) List<Long> payslipIds,
                                 @RequestParam(value = "currentYear", required = false) String currentYear,
                                 RedirectAttributes redirectAttributes) {
        YearMonth month = resolveMonth(referenceMonth);
        int deleted = payslipService.deletePayslips(payslipIds);
        String message = deleted > 0
                ? String.format("Eliminati %d cedolini selezionati", deleted)
                : "Nessun cedolino selezionato";
        redirectAttributes.addFlashAttribute("deleteMessage", message);
        Year year = resolveYear(currentYear);
        return "redirect:/payslips?month=" + month.format(MONTH_INPUT_FORMATTER)
                + "&year=" + year.format(YEAR_FORMATTER);
    }

    // --- SEZIONE GESTIONE CERTIFICAZIONI UNICHE ---
    // NOTA: Questi metodi erano fuori dalla classe e sono stati spostati qui.

    @PostMapping("/certifications/upload")
    public String uploadCertifications(@RequestParam("referenceYear") String referenceYear,
                                       @RequestParam(value = "files", required = false) MultipartFile[] files,
                                       @RequestParam(value = "currentMonth", required = false) String currentMonth,
                                       RedirectAttributes redirectAttributes) {
        Year year = resolveYear(referenceYear);
        YearMonth month = resolveMonth(currentMonth);
        // NOTA: Sarebbe meglio usare un DTO specifico come "CertificationUploadResult"
        PayslipUploadResult result = uniqueCertificationService.uploadCertifications(year, files);
        redirectAttributes.addFlashAttribute("certificationUploadResult", result);
        redirectAttributes.addFlashAttribute("selectedYear", year);
        return "redirect:/payslips?month=" + month.format(MONTH_INPUT_FORMATTER)
                + "&year=" + year.format(YEAR_FORMATTER);
    }

    @PostMapping("/certifications/send")
    public String sendCertifications(@RequestParam("referenceYear") String referenceYear,
                                     @RequestParam(value = "certificationIds", required = false) List<Long> certificationIds,
                                     @RequestParam(value = "subject", required = false) String subject,
                                     @RequestParam(value = "body", required = false) String body,
                                     @RequestParam(value = "currentMonth", required = false) String currentMonth,
                                     RedirectAttributes redirectAttributes) {
        Year year = resolveYear(referenceYear);
        YearMonth month = resolveMonth(currentMonth);
        String sanitizedSubject = subject != null ? subject.trim() : "";
        String sanitizedBody = body != null ? body.trim() : "";
        // NOTA: Sarebbe meglio usare un DTO specifico come "CertificationSendResult"
        PayslipSendResult result = uniqueCertificationEmailService.sendCertifications(
                certificationIds == null ? List.of() : certificationIds,
                sanitizedSubject,
                sanitizedBody);
        redirectAttributes.addFlashAttribute("certificationSendResult", result);
        redirectAttributes.addFlashAttribute("lastCertificationSubject", sanitizedSubject);
        redirectAttributes.addFlashAttribute("lastCertificationBody", sanitizedBody);
        return "redirect:/payslips?month=" + month.format(MONTH_INPUT_FORMATTER)
                + "&year=" + year.format(YEAR_FORMATTER);
    }

    @GetMapping("/certifications/{id}/download")
    public ResponseEntity<Resource> downloadCertification(@PathVariable Long id) {
        Resource resource = uniqueCertificationService.loadCertificationFile(id);
        String filename = resource.getFilename();
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(filename != null ? filename : "certificazione-unica.pdf")
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @PostMapping("/certifications/{id}/delete")
    public String deleteCertification(@PathVariable Long id,
                                      @RequestParam("referenceYear") String referenceYear,
                                      @RequestParam(value = "currentMonth", required = false) String currentMonth,
                                      RedirectAttributes redirectAttributes) {
        Year year = resolveYear(referenceYear);
        YearMonth month = resolveMonth(currentMonth);
        uniqueCertificationService.deleteCertification(id);
        redirectAttributes.addFlashAttribute("certificationDeleteMessage", "Certificazione Unica eliminata con successo");
        return "redirect:/payslips?month=" + month.format(MONTH_INPUT_FORMATTER)
                + "&year=" + year.format(YEAR_FORMATTER);
    }

    @PostMapping("/certifications/delete-selected")
    public String deleteSelectedCertifications(@RequestParam("referenceYear") String referenceYear,
                                               @RequestParam(value = "certificationIds", required = false) List<Long> certificationIds,
                                               @RequestParam(value = "currentMonth", required = false) String currentMonth,
                                               RedirectAttributes redirectAttributes) {
        Year year = resolveYear(referenceYear);
        YearMonth month = resolveMonth(currentMonth);
        int deleted = uniqueCertificationService.deleteCertifications(certificationIds);
        String message = deleted > 0
                ? String.format("Eliminate %d Certificazioni Uniche selezionate", deleted)
                : "Nessuna Certificazione Unica selezionata";
        redirectAttributes.addFlashAttribute("certificationDeleteMessage", message);
        return "redirect:/payslips?month=" + month.format(MONTH_INPUT_FORMATTER)
                + "&year=" + year.format(YEAR_FORMATTER);
    }

    // --- METODI PRIVATI DI UTILITÀ ---
    // NOTA: Anche questi metodi erano fuori dalla classe e sono stati spostati qui.

    /**
     * Converte una stringa nel formato "yyyy-MM" in un oggetto YearMonth.
     * Se la stringa è nulla, vuota o non valida, restituisce il mese corrente.
     */
    private YearMonth resolveMonth(String month) {
        if (StringUtils.hasText(month)) {
            try {
                return YearMonth.parse(month, MONTH_INPUT_FORMATTER);
            } catch (DateTimeParseException ex) { // CORREZIONE: Intercetto l'eccezione specifica
                log.warn("Formato mese non valido '{}', uso il mese corrente", month);
            }
        }
        return YearMonth.now();
    }

    /**
     * Converte una stringa nel formato "yyyy" in un oggetto Year.
     * Se la stringa è nulla, vuota o non valida, restituisce l'anno corrente.
     */
    private Year resolveYear(String year) {
        if (StringUtils.hasText(year)) {
            try {
                return Year.parse(year, YEAR_FORMATTER);
            } catch (DateTimeParseException ex) { // CORREZIONE: Intercetto l'eccezione specifica
                log.warn("Formato anno non valido '{}', uso l'anno corrente", year);
            }
        }
        return Year.now();
    }
}