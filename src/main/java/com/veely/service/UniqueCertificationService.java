package com.veely.service;

import com.veely.dto.payslip.PayslipUploadResult;
import com.veely.entity.Employee;
import com.veely.entity.UniqueCertification;
import com.veely.model.PayslipStatus;
import com.veely.repository.EmployeeRepository;
import com.veely.repository.UniqueCertificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UniqueCertificationService {

    private final UniqueCertificationRepository uniqueCertificationRepository;
    private final EmployeeRepository employeeRepository;
    private final SecureFileUploadService secureFileUploadService;
    private final FileSystemStorageService fileSystemStorageService;

    public PayslipUploadResult uploadCertifications(Year referenceYear, MultipartFile[] files) {
        PayslipUploadResult result = PayslipUploadResult.builder().build();
        if (files == null || files.length == 0) {
            return result;
        }

        String subdir = String.format(Locale.ITALY, "unique-certifications/%d", referenceYear.getValue());

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
                UniqueCertification certification = UniqueCertification.builder()
                        .employee(employee)
                        .fiscalCode(fiscalCode)
                        .referenceYear(referenceYear.getValue())
                        .storagePath(subdir + "/" + storedFilename)
                        .originalFilename(originalFilename)
                        .uploadedAt(LocalDateTime.now())
                        .status(employee == null ? PayslipStatus.UNMATCHED : PayslipStatus.PENDING)
                        .build();
                uniqueCertificationRepository.save(certification);
                result.setStored(result.getStored() + 1);

                if (employee == null) {
                    result.addUnmatched(fiscalCode + " (" + originalFilename + ")");
                }
            } catch (IOException ex) {
                log.error("Errore durante il caricamento della Certificazione Unica {}", originalFilename, ex);
                result.addError(originalFilename + ": " + ex.getMessage());
            } catch (Exception ex) {
                log.error("Errore non previsto durante il caricamento della Certificazione Unica {}", originalFilename, ex);
                result.addError(originalFilename + ": " + ex.getMessage());
            }
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<UniqueCertification> findByReferenceYear(Year referenceYear) {
        List<UniqueCertification> certifications =
                uniqueCertificationRepository.findByReferenceYearOrderByUploadedAtDesc(referenceYear.getValue());
        certifications.sort(Comparator.comparing(UniqueCertification::getDisplayName, String.CASE_INSENSITIVE_ORDER));
        return certifications;
    }

    @Transactional(readOnly = true)
    public List<Year> getAvailableYears() {
        List<Year> years = uniqueCertificationRepository.findAvailableYears().stream()
                .map(Year::of)
                .toList();
        if (years.isEmpty()) {
            return List.of(Year.now());
        }
        if (years.stream().noneMatch(year -> year.equals(Year.now()))) {
            List<Year> enriched = new ArrayList<>(years);
            enriched.add(Year.now());
            enriched.sort(Comparator.reverseOrder());
            return enriched;
        }
        return years;
    }

    @Transactional(readOnly = true)
    public Resource loadCertificationFile(Long certificationId) {
        UniqueCertification certification = uniqueCertificationRepository.findById(certificationId)
                .orElseThrow(() -> new IllegalArgumentException("Certificazione Unica non trovata: " + certificationId));
        Path path = Path.of(certification.getStoragePath());
        String filename = path.getFileName().toString();
        String subdir = Optional.ofNullable(path.getParent()).map(Path::toString).orElse("");
        return fileSystemStorageService.loadAsResource(filename, subdir);
    }

    public void deleteCertification(Long certificationId) {
        UniqueCertification certification = uniqueCertificationRepository.findById(certificationId)
                .orElseThrow(() -> new IllegalArgumentException("Certificazione Unica non trovata: " + certificationId));
        fileSystemStorageService.delete(certification.getStoragePath());
        uniqueCertificationRepository.delete(certification);
    }

    public int deleteCertifications(Collection<Long> certificationIds) {
        if (certificationIds == null || certificationIds.isEmpty()) {
            return 0;
        }

        List<UniqueCertification> certificationsToDelete = uniqueCertificationRepository.findByIdIn(certificationIds);
        certificationsToDelete.forEach(certification ->
                fileSystemStorageService.delete(certification.getStoragePath()));
        uniqueCertificationRepository.deleteAll(certificationsToDelete);
        return certificationsToDelete.size();
    }

    @Transactional(readOnly = true)
    public List<UniqueCertification> findByIds(Collection<Long> ids) {
        return uniqueCertificationRepository.findByIdIn(ids);
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
