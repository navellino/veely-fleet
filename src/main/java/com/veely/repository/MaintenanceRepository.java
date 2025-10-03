package com.veely.repository;

import com.veely.entity.Maintenance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface MaintenanceRepository extends JpaRepository<Maintenance, Long> {
    List<Maintenance> findByVehicleId(Long vehicleId);
    
    @Query("""
            select m from Maintenance m
            where (:plate is null or lower(m.vehicle.plate) like lower(concat('%', :plate, '%')))
              and (:year is null or year(m.date) = :year)
              and (:typeId is null or m.type.id = :typeId)
            order by m.date desc
            """)
    List<Maintenance> search(@Param("plate") String plate,
                             @Param("year") Integer year,
                             @Param("typeId") Long typeId);
    
    @Query("select coalesce(sum(m.cost), 0) from Maintenance m where year(m.date) = :year")
    java.math.BigDecimal sumCostYear(@Param("year") int year);

    @Query("select avg(m.cost) from Maintenance m where year(m.date) = :year")
    Double avgCostYear(@Param("year") int year);
    
 // ELENCO PER ANNO -> via range
    List<Maintenance> findAllByDateBetween(LocalDate start, LocalDate end);

    // KPI via range (DB-agnostico)
    @Query("""
           select coalesce(sum(m.cost), 0)
           from Maintenance m
           where m.date between :start and :end
           """)
    BigDecimal sumCostBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("""
           select coalesce(avg(m.cost), 0)
           from Maintenance m
           where m.date between :start and :end
           """)
    Double avgCostBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    // SEARCH con filtri opzionali (plate e typeId) + range anno
    @Query("""
           select m
           from Maintenance m
           where m.date between :start and :end
             and (:plate is null or :plate = '' or upper(m.vehicle.plate) like concat('%', upper(:plate), '%'))
             and (:typeId is null or m.type.id = :typeId)
           order by m.date desc
           """)
    List<Maintenance> searchByFilters(@Param("start") LocalDate start,
                                      @Param("end") LocalDate end,
                                      @Param("plate") String plate,
                                      @Param("typeId") Long typeId);
    
}
