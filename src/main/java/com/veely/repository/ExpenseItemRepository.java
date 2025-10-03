package com.veely.repository;

import com.veely.entity.ExpenseItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ExpenseItemRepository extends JpaRepository<ExpenseItem, Long> {
    List<ExpenseItem> findByExpenseReportId(Long reportId);
    
    @Query("select year(e.date) as yr, month(e.date) as mth, sum(e.amount) " +
            "from ExpenseItem e where e.date between :start and :end " +
            "group by year(e.date), month(e.date) " +
            "order by yr, mth")
     List<Object[]> sumByMonth(@Param("start") LocalDate start,
                               @Param("end") LocalDate end);
}
