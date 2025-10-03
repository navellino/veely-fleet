package com.veely.repository;

import com.veely.entity.Payslip;
import com.veely.model.PayslipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface PayslipRepository extends JpaRepository<Payslip, Long> {

    List<Payslip> findByReferenceMonthOrderByUploadedAtDesc(LocalDate referenceMonth);

    List<Payslip> findByIdIn(Collection<Long> ids);

    List<Payslip> findByReferenceMonthAndStatus(LocalDate referenceMonth, PayslipStatus status);

    @Query("select distinct p.referenceMonth from Payslip p order by p.referenceMonth desc")
    List<LocalDate> findAvailableMonths();
}
