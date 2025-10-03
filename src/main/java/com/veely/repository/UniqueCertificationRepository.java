package com.veely.repository;

import com.veely.entity.UniqueCertification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface UniqueCertificationRepository extends JpaRepository<UniqueCertification, Long> {

    List<UniqueCertification> findByReferenceYearOrderByUploadedAtDesc(Integer referenceYear);

    List<UniqueCertification> findByIdIn(Collection<Long> ids);

    @Query("select distinct uc.referenceYear from UniqueCertification uc order by uc.referenceYear desc")
    List<Integer> findAvailableYears();
}
