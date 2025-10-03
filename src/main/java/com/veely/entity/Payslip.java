package com.veely.entity;

import com.veely.model.PayslipStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payslips")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payslip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Column(name = "fiscal_code", length = 16, nullable = false)
    private String fiscalCode;

    @Column(name = "reference_month", nullable = false)
    private LocalDate referenceMonth;

    @Column(name = "storage_path", nullable = false)
    private String storagePath;

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayslipStatus status;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "sent_to")
    private String sentTo;

    @Column(name = "last_subject", length = 255)
    private String lastSubject;

    @Lob
    @Column(name = "last_body", length = 1000)
    private String lastBody;

    @Lob
    @Column(name = "last_error", columnDefinition="TEXT")
    private String lastError;

    public String getDisplayName() {
        if (employee != null) {
            return employee.getFullName();
        }
        return fiscalCode;
    }
}
