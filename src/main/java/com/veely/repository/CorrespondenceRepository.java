package com.veely.repository;

import com.veely.entity.Correspondence;
import com.veely.model.CorrespondenceType;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CorrespondenceRepository extends JpaRepository<Correspondence, Long> {
    @Query("select max(c.progressivo) from Correspondence c where c.anno = :anno and c.tipo = :tipo")
    Integer findMaxProgressivo(@Param("anno") int anno, @Param("tipo") CorrespondenceType tipo);
    
    List<Correspondence> findByAnnoOrderByProgressivoDesc(int anno);

    @Query("""
            select c from Correspondence c
            where c.anno = :anno and (
                  lower(c.descrizione) like lower(concat('%', :kw, '%')) or
                  lower(c.sender) like lower(concat('%', :kw, '%')) or
                  lower(coalesce(c.recipient,'')) like lower(concat('%', :kw, '%'))
            )
            order by c.progressivo desc
            """)
    List<Correspondence> searchByAnnoAndKeyword(@Param("anno") int anno, @Param("kw") String keyword);
    
    List<Correspondence> findByAnnoAndTipoOrderByProgressivoDesc(int anno, CorrespondenceType tipo);
    
    @Query("""
            select c from Correspondence c
            where c.anno = :anno and c.tipo = :tipo and (
                  lower(c.descrizione) like lower(concat('%', :kw, '%')) or
                  lower(c.sender) like lower(concat('%', :kw, '%')) or
                  lower(coalesce(c.recipient,'')) like lower(concat('%', :kw, '%'))
            )
            order by c.progressivo desc
            """)
    List<Correspondence> searchByAnnoAndTipoAndKeyword(@Param("anno") int anno,
                                                       @Param("tipo") CorrespondenceType tipo,
                                                       @Param("kw") String keyword);
    
    Optional<Correspondence> findFirstByAnnoAndTipoOrderByProgressivoDesc(int anno, CorrespondenceType tipo);
    
    @Query("select distinct c.anno from Correspondence c order by c.anno desc")
    List<Integer> findDistinctAnni();
}
