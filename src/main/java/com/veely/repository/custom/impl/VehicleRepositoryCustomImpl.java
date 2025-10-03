package com.veely.repository.custom.impl;

import com.veely.entity.Vehicle;
import com.veely.model.AssignmentStatus;
import com.veely.model.VehicleStatus;
import com.veely.repository.custom.VehicleRepositoryCustom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class VehicleRepositoryCustomImpl implements VehicleRepositoryCustom {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Override
    public List<Vehicle> findAllWithRelations() {
        return entityManager.createQuery(
            "SELECT DISTINCT v FROM Vehicle v " +
            "LEFT JOIN FETCH v.supplier " +
            "LEFT JOIN FETCH v.fuelCard " +
            "LEFT JOIN FETCH v.assignments a " +
            "LEFT JOIN FETCH a.employment e " +
            "LEFT JOIN FETCH e.employee " +
            "ORDER BY v.plate", Vehicle.class)
            .getResultList();
    }
    
    @Override
    public Page<Vehicle> findByStatusOptimized(VehicleStatus status, Pageable pageable) {
        // Count query
        Long total = entityManager.createQuery(
            "SELECT COUNT(v) FROM Vehicle v WHERE v.status = :status", Long.class)
            .setParameter("status", status)
            .getSingleResult();
        
        // Data query con fetch
        List<Vehicle> vehicles = entityManager.createQuery(
            "SELECT DISTINCT v FROM Vehicle v " +
            "LEFT JOIN FETCH v.supplier " +
            "LEFT JOIN FETCH v.fuelCard " +
            "WHERE v.status = :status " +
            "ORDER BY v.plate", Vehicle.class)
            .setParameter("status", status)
            .setFirstResult((int) pageable.getOffset())
            .setMaxResults(pageable.getPageSize())
            .getResultList();
        
        return new PageImpl<>(vehicles, pageable, total);
    }
    
    @Override
    public Page<Vehicle> searchVehicles(String plate, String brand, VehicleStatus status,
                                       LocalDate expiryDateFrom, LocalDate expiryDateTo,
                                       Pageable pageable) {
        
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        
        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Vehicle> countRoot = countQuery.from(Vehicle.class);
        countQuery.select(cb.count(countRoot));
        List<Predicate> countPredicates = buildPredicates(cb, countRoot, plate, brand, status, 
                                                         expiryDateFrom, expiryDateTo);
        if (!countPredicates.isEmpty()) {
            countQuery.where(cb.and(countPredicates.toArray(new Predicate[0])));
        }
        Long total = entityManager.createQuery(countQuery).getSingleResult();
        
        // Data query
        CriteriaQuery<Vehicle> dataQuery = cb.createQuery(Vehicle.class);
        Root<Vehicle> root = dataQuery.from(Vehicle.class);
        root.fetch("supplier", JoinType.LEFT);
        root.fetch("fuelCard", JoinType.LEFT);
        
        dataQuery.select(root).distinct(true);
        
        List<Predicate> predicates = buildPredicates(cb, root, plate, brand, status, 
                                                    expiryDateFrom, expiryDateTo);
        if (!predicates.isEmpty()) {
            dataQuery.where(cb.and(predicates.toArray(new Predicate[0])));
        }
        
        // Applica ordinamento
        List<Order> orders = new ArrayList<>();
        pageable.getSort().forEach(order -> {
            if (order.isAscending()) {
                orders.add(cb.asc(root.get(order.getProperty())));
            } else {
                orders.add(cb.desc(root.get(order.getProperty())));
            }
        });
        if (!orders.isEmpty()) {
            dataQuery.orderBy(orders);
        }
        
        TypedQuery<Vehicle> query = entityManager.createQuery(dataQuery);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        
        List<Vehicle> vehicles = query.getResultList();
        
        return new PageImpl<>(vehicles, pageable, total);
    }
    
    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<Vehicle> root,
                                           String plate, String brand, VehicleStatus status,
                                           LocalDate expiryDateFrom, LocalDate expiryDateTo) {
        List<Predicate> predicates = new ArrayList<>();
        
        if (plate != null && !plate.isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("plate")), 
                                 "%" + plate.toLowerCase() + "%"));
        }
        
        if (brand != null && !brand.isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("brand")), 
                                 "%" + brand.toLowerCase() + "%"));
        }
        
        if (status != null) {
            predicates.add(cb.equal(root.get("status"), status));
        }
        
        if (expiryDateFrom != null || expiryDateTo != null) {
            Predicate insurancePredicate = null;
            Predicate carTaxPredicate = null;
            
            if (expiryDateFrom != null && expiryDateTo != null) {
                insurancePredicate = cb.between(root.get("insuranceExpiryDate"), 
                                              expiryDateFrom, expiryDateTo);
                carTaxPredicate = cb.between(root.get("carTaxExpiryDate"), 
                                           expiryDateFrom, expiryDateTo);
            } else if (expiryDateFrom != null) {
                insurancePredicate = cb.greaterThanOrEqualTo(root.get("insuranceExpiryDate"), 
                                                            expiryDateFrom);
                carTaxPredicate = cb.greaterThanOrEqualTo(root.get("carTaxExpiryDate"), 
                                                         expiryDateFrom);
            } else {
                insurancePredicate = cb.lessThanOrEqualTo(root.get("insuranceExpiryDate"), 
                                                         expiryDateTo);
                carTaxPredicate = cb.lessThanOrEqualTo(root.get("carTaxExpiryDate"), 
                                                      expiryDateTo);
            }
            
            predicates.add(cb.or(insurancePredicate, carTaxPredicate));
        }
        
        return predicates;
    }
    
    @Override
    public List<VehicleStatistics> getVehicleStatistics() {
        return entityManager.createQuery(
            "SELECT new com.veely.repository.custom.VehicleRepositoryCustom$VehicleStatistics(" +
            "v.brand, " +
            "COUNT(v), " +
            "AVG(v.currentMileage), " +
            "SUM(CASE WHEN a.status = :activeStatus THEN 1 ELSE 0 END)) " +
            "FROM Vehicle v " +
            "LEFT JOIN v.assignments a " +
            "GROUP BY v.brand " +
            "ORDER BY COUNT(v) DESC", VehicleStatistics.class)
            .setParameter("activeStatus", AssignmentStatus.ASSIGNED)
            .getResultList();
    }
}
