package com.veely.service;

import com.veely.dto.payslip.PayslipUploadResult;
import com.veely.entity.Employee;
import com.veely.entity.Payslip;
import com.veely.model.PayslipStatus;
import com.veely.repository.EmployeeRepository;
import com.veely.repository.PayslipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PayslipService {

    private final PayslipRepository payslipRepository;
    private final EmployeeRepository employeeRepository;
    private final SecureFileUploadService secureFileUploadService;
    private final FileSystemStorageService fileSystemStorageService;

    public PayslipUploadResult uploadPayslips(YearMonth referenceMonth, MultipartFile[] files) {
        PayslipUploadResult result = PayslipUploadResult.builder().build();
        if (files == null || files.length == 0) {
            return result;
        }

        LocalDate monthDate = referenceMonth.atDay(1);
        String subdir = String.format(Locale.ITALY, "payslips/%d/%02d",
                referenceMonth.getYear(), referenceMonth.getMonthValue());

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            result.setProcessed(result.getProcessed() + 1);
            String originalFilename = file.getOriginalFilename();
            try {
                String fiscalCode = extractFiscalCode(originalFilename);
                if (!StringUtils.hasText(fiscalCode)) {
                    result.addError(originalFilename + ": impossibile leggere il codice fiscale dal nome file");
                    continue;
                }

                Optional<Employee> employeeOpt = employeeRepository.findByFiscalCodeIgnoreCase(fiscalCode);
                Employee employee = employeeOpt.orElse(null);

                String storedFilename = secureFileUploadService.uploadDocument(file, subdir);
                Payslip payslip = Payslip.builder()
                        .employee(employee)
                        .fiscalCode(fiscalCode)
                        .referenceMonth(monthDate)
                        .storagePath(subdir + "/" + storedFilename)
                        .originalFilename(originalFilename)
                        .uploadedAt(LocalDateTime.now())
                        .status(employee == null ? PayslipStatus.UNMATCHED : PayslipStatus.PENDING)
                        .build();
                payslipRepository.save(payslip);
                result.setStored(result.getStored() + 1);

                if (employee == null) {
                    result.addUnmatched(fiscalCode + " (" + originalFilename + ")");
                }
            } catch (IOException ex) {
                log.error("Errore durante il caricamento del cedolino {}", originalFilename, ex);
                result.addError(originalFilename + ": " + ex.getMessage());
            } catch (Exception ex) {
                log.error("Errore non previsto durante il caricamento del cedolino {}", originalFilename, ex);
                result.addError(originalFilename + ": " + ex.getMessage());
            }
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<Payslip> findByReferenceMonth(YearMonth referenceMonth) {
        LocalDate monthDate = referenceMonth.atDay(1);
        List<Payslip> payslips = payslipRepository.findByReferenceMonthOrderByUploadedAtDesc(monthDate);
        payslips.sort(Comparator.comparing(Payslip::getDisplayName, String.CASE_INSENSITIVE_ORDER));
        return payslips;
    }
    
    @Transactional(readOnly = true)
    public List<Payslip> findSentByReferenceMonth(YearMonth referenceMonth) {
        LocalDate monthDate = referenceMonth.atDay(1);
        List<Payslip> payslips = payslipRepository.findByReferenceMonthAndStatus(monthDate, PayslipStatus.SENT);
        payslips.sort(Comparator.comparing(Payslip::getDisplayName, String.CASE_INSENSITIVE_ORDER));
        return payslips;
    }

    @Transactional(readOnly = true)
    public List<YearMonth> getAvailableMonths() {
        return payslipRepository.findAvailableMonths().stream()
                .map(YearMonth::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Resource loadPayslipFile(Long payslipId) {
        Payslip payslip = payslipRepository.findById(payslipId)
                .orElseThrow(() -> new IllegalArgumentException("Cedolino non trovato: " + payslipId));
        Path path = Path.of(payslip.getStoragePath());
        String filename = path.getFileName().toString();
        String subdir = Optional.ofNullable(path.getParent()).map(Path::toString).orElse("");
        return fileSystemStorageService.loadAsResource(filename, subdir);
    }

    public void deletePayslip(Long payslipId) {
        Payslip payslip = payslipRepository.findById(payslipId)
                .orElseThrow(() -> new IllegalArgumentException("Cedolino non trovato: " + payslipId));
        fileSystemStorageService.delete(payslip.getStoragePath());
        payslipRepository.delete(payslip);
    }
    
    public int deletePayslips(Collection<Long> payslipIds) {
        if (payslipIds == null || payslipIds.isEmpty()) {
            return 0;
        }

        List<Payslip> payslipsToDelete = payslipRepository.findByIdIn(payslipIds);
        payslipsToDelete.forEach(payslip -> fileSystemStorageService.delete(payslip.getStoragePath()));
        payslipRepository.deleteAll(payslipsToDelete);
        return payslipsToDelete.size();
    }

    @Transactional(readOnly = true)
    public List<Payslip> findByIds(Collection<Long> ids) {
        return payslipRepository.findByIdIn(ids);
    }

    private String extractFiscalCode(String filename) {
        if (!StringUtils.hasText(filename)) {
            return null;
        }
        String baseName = Path.of(filename).getFileName().toString();
        int dotIndex = baseName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = baseName.substring(0, dotIndex);
        }
        String cleaned = baseName.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ITALY);
        if (cleaned.length() > 16) {
            cleaned = cleaned.substring(0, 16);
        }
        return cleaned;
    }
}
