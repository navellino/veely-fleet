package com.veely.service;

import com.veely.model.AssignmentStatus;
import com.veely.model.ExpenseStatus;
import com.veely.model.SupplierContractStatus;
import com.veely.model.VehicleStatus;
import com.veely.entity.ComplianceItem;
import com.veely.entity.Project;
import com.veely.entity.VehicleTask;
import com.veely.model.TaskStatus;
import com.veely.repository.VehicleTaskRepository;
import com.veely.repository.AssignmentRepository;
import com.veely.repository.ContractRepository;
import com.veely.repository.ExpenseReportRepository;
import com.veely.repository.RefuelRepository;
import com.veely.repository.VehicleRepository;
import com.veely.repository.InsuranceRepository;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.data.domain.Sort;


@Service
@RequiredArgsConstructor
public class DashboardService {

    private final VehicleRepository vehicleRepo;
    private final AssignmentRepository assignmentRepo;
    private final CorrespondenceService correspondenceService;
    private final ExpenseReportRepository expenseReportRepo;
    private final RefuelRepository refuelRepo;
    private final VehicleTaskRepository vehicleTaskRepo;
    private final ContractRepository contractRepo;
    private final ComplianceItemService complianceItemService;
    private final ProjectService projectService;
    private final AdminDocumentService adminDocumentService;
    private final InsuranceRepository insuranceRepository;


    public DashboardMetrics getMetrics() {
        long vehicles = vehicleRepo.count();
        long inService = vehicleRepo.countByStatus(VehicleStatus.IN_SERVICE);
        long assigned = assignmentRepo.countDistinctVehicleByStatus(AssignmentStatus.ASSIGNED);
        long totalAssignments = assignmentRepo.count();
        String lastIncoming = correspondenceService.getLastIncomingProtocol();
        String lastOutgoing = correspondenceService.getLastOutgoingProtocol();
        
        YearMonth ym = java.time.YearMonth.now();
       	LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        BigDecimal fuelMonth = refuelRepo.sumAmountBetween(start, end);
        
        List<Project> activeProjects = projectService.findActive();
        long activeProjectCount = activeProjects.size();
        BigDecimal activeProjectsValue = activeProjects.stream()
                .map(Project::getValue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        long activeContracts = contractRepo.countByStatus(SupplierContractStatus.IN_ESECUZIONE);
        
        return new DashboardMetrics(vehicles, inService, assigned,
        		totalAssignments, lastIncoming, lastOutgoing, fuelMonth,
                activeProjectCount, activeProjectsValue, activeContracts);
    }
    
    /** Returns vehicle counts grouped by status. */
    public java.util.Map<String, Long> getVehicleStatusCounts() {
        java.util.Map<String, Long> map = new java.util.LinkedHashMap<>();
        for (VehicleStatus vs : VehicleStatus.values()) {
            map.put(vs.getDisplayName(), vehicleRepo.countByStatus(vs));
        }
        return map;
    }

    /** Last {@code months} monthly fuel costs aggregated from expense items. */
    public List<MonthAmount> getFuelCosts(int months) {
        java.time.YearMonth start = java.time.YearMonth.now().minusMonths(months - 1);
        java.time.LocalDate from = start.atDay(1);
        LocalDate to = LocalDate.now();
        Map<java.time.YearMonth, java.math.BigDecimal> tmp = new java.util.LinkedHashMap<>();
        for (Object[] row : refuelRepo.sumAmountByMonth(from, to)) {
            int y = ((Number) row[0]).intValue();
            int m = ((Number) row[1]).intValue();
            BigDecimal sum = new java.math.BigDecimal(row[2].toString());
            tmp.put(java.time.YearMonth.of(y, m), sum);
        }
        List<MonthAmount> result = new java.util.ArrayList<>();
        for (int i = 0; i < months; i++) {
        	YearMonth ym = start.plusMonths(i);
        	BigDecimal val = tmp.getOrDefault(ym, BigDecimal.ZERO);
        	result.add(new MonthAmount(ym.toString(), val));
        }
        return result;
    }

    /** Last {@code months} reimbursable expense report balances. */
    public List<MonthAmount> getExpenseReportBalances(int months) {
        YearMonth start = YearMonth.now().minusMonths(months - 1);
        LocalDate from = start.atDay(1);
        java.time.LocalDate to = java.time.LocalDate.now();
        Map<YearMonth, java.math.BigDecimal> tmp = new LinkedHashMap<>();
        for (Object[] row : expenseReportRepo.sumBalancesByMonth(from, to)) {
            int y = ((Number) row[0]).intValue();
            int m = ((Number) row[1]).intValue();
            BigDecimal sum = new java.math.BigDecimal(row[2].toString());
            tmp.put(YearMonth.of(y, m), sum);
        }
        List<MonthAmount> result = new java.util.ArrayList<>();
        for (int i = 0; i < months; i++) {
            YearMonth ym = start.plusMonths(i);
            BigDecimal val = tmp.getOrDefault(ym, java.math.BigDecimal.ZERO);
            result.add(new MonthAmount(ym.toString(), val));
        }
        return result;
    }

    /**
     * Upcoming vehicle maintenance tasks taking into account mileage and
     * date thresholds. A task is included if either the due mileage has
     * been reached or the due date is within the next 60 days.
     */
    
    public List<VehicleTask> getUpcomingTasks(int limit) {
        return vehicleTaskRepo.findByStatusOrderByDueDateAsc(TaskStatus.OPEN)
                .stream()
                .sorted(java.util.Comparator.comparing(VehicleTask::getDueDate,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .limit(limit)
                .toList();
    }
        

    /** Upcoming safety compliance items sorted by due date */
    public List<ComplianceItem> getUpcomingComplianceItems(int limit) {
	return complianceItemService
                .search(null, null, null, null, null, false)
                .stream()
                .filter(i -> i.getDueDate() != null)
                .sorted(java.util.Comparator.comparing(com.veely.entity.ComplianceItem::getDueDate))
                .limit(limit)
                .toList();
    }
    
    /** Upcoming administrative documents ordered by due date */
    public List<com.veely.entity.AdminDocument> getUpcomingAdminDocuments(int limit) {
        java.time.LocalDate today = java.time.LocalDate.now();
        return adminDocumentService.findAll().stream()
                .filter(d -> d.getExpiryDate() != null && !d.getExpiryDate().isBefore(today))
                .sorted(java.util.Comparator.comparing(com.veely.entity.AdminDocument::getExpiryDate))
                .limit(limit)
                .toList();
    }
    
    /** Latest expense reports waiting for approval. */
    public List<com.veely.entity.ExpenseReport> getPendingExpenseReports(int limit) {
        return expenseReportRepo
                .findTop5ByExpenseStatusOrderByReportSubmitDateDesc(ExpenseStatus.Submitted)
                .stream()
                .limit(limit)
                .toList();
    }

    /** Upcoming project insurance policies ordered by expiry date */
    public List<com.veely.entity.Insurance> getExpiringPolicies(int limit) {
        java.time.LocalDate today = java.time.LocalDate.now();
        return insuranceRepository.findAll(Sort.by(Sort.Direction.ASC, "expiryDate"))
                .stream()
                .filter(p -> p.getExpiryDate() != null && !p.getExpiryDate().isBefore(today))
                .limit(limit)
                .toList();
    }

    
    public record MonthAmount(String month, BigDecimal total) {}

    public record DashboardMetrics(long vehicles,
            long vehiclesInService,
            long vehiclesAssigned,
            long assignments,
            String lastIncomingProtocol,
            String lastOutgoingProtocol,
            BigDecimal fuelMonth,
            long activeProjects,
            BigDecimal activeProjectsValue,
            long activeContracts) {}
}
