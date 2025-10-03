package com.veely.repository;

import com.veely.entity.VehicleBooking;
import com.veely.model.VehicleBookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface VehicleBookingRepository extends JpaRepository<VehicleBooking, Long> {

    List<VehicleBooking> findByVehicleIdOrderByStartDateTimeAsc(Long vehicleId);

    @Query("SELECT b FROM VehicleBooking b " +
            "WHERE b.vehicle.id = :vehicleId " +
            "AND (:from IS NULL OR b.endDateTime >= :from) " +
            "AND (:to IS NULL OR b.startDateTime <= :to) " +
            "ORDER BY b.startDateTime")
    List<VehicleBooking> findForVehicleAndRange(@Param("vehicleId") Long vehicleId,
                                                @Param("from") LocalDateTime from,
                                                @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(b) FROM VehicleBooking b " +
            "WHERE b.status <> :cancelled " +
            "AND b.endDateTime >= :now")
    long countActive(@Param("cancelled") VehicleBookingStatus cancelled,
                     @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(b) FROM VehicleBooking b " +
            "WHERE b.status <> :cancelled " +
            "AND b.startDateTime <= :endOfDay " +
            "AND b.endDateTime >= :startOfDay")
    long countForDate(@Param("cancelled") VehicleBookingStatus cancelled,
                      @Param("startOfDay") LocalDateTime startOfDay,
                      @Param("endOfDay") LocalDateTime endOfDay);

    @Query("SELECT COUNT(b) FROM VehicleBooking b " +
            "WHERE b.status <> :cancelled " +
            "AND b.startDateTime BETWEEN :from AND :to")
    long countStartingBetween(@Param("cancelled") VehicleBookingStatus cancelled,
                              @Param("from") LocalDateTime from,
                              @Param("to") LocalDateTime to);

    void deleteByVehicleId(Long vehicleId);
}
