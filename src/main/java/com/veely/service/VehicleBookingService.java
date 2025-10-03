package com.veely.service;

import com.veely.dto.vehicle.VehicleBookingRequest;
import com.veely.dto.vehicle.VehicleBookingResponse;
import com.veely.entity.Assignment;
import com.veely.entity.Vehicle;
import com.veely.entity.VehicleBooking;
import com.veely.exception.BusinessValidationException;
import com.veely.exception.ResourceNotFoundException;
import com.veely.model.AssignmentStatus;
import com.veely.model.VehicleBookingStatus;
import com.veely.model.VehicleStatus;
import com.veely.repository.AssignmentRepository;
import com.veely.repository.VehicleBookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class VehicleBookingService {

    private static final LocalDate FAR_FUTURE_DATE = LocalDate.of(9999, 12, 31);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final VehicleBookingRepository bookingRepository;
    private final VehicleService vehicleService;
    private final AssignmentRepository assignmentRepository;

    @Transactional(readOnly = true)
    public List<VehicleBookingResponse> findByVehicle(Long vehicleId, LocalDate from, LocalDate to) {
        if (vehicleId == null) {
            return List.of();
        }
        LocalDateTime fromDateTime = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDateTime = to != null ? to.atTime(LocalTime.MAX) : null;
        return bookingRepository.findForVehicleAndRange(vehicleId, fromDateTime, toDateTime)
                .stream()
                .sorted(Comparator.comparing(VehicleBooking::getStartDateTime))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public VehicleBookingResponse create(VehicleBookingRequest request) {
        log.info("Creazione prenotazione veicolo: vehicleId={}, start={}, end={}",
                request.getVehicleId(), request.getStartDateTime(), request.getEndDateTime());
        Vehicle vehicle = vehicleService.findByIdOrThrow(request.getVehicleId());
        validateBookingData(vehicle, request.getStartDateTime(), request.getEndDateTime(), null);

        VehicleBooking booking = VehicleBooking.builder()
                .vehicle(vehicle)
                .startDateTime(request.getStartDateTime())
                .endDateTime(request.getEndDateTime())
                .title(request.getTitle())
                .requesterName(request.getRequesterName())
                .requesterContact(request.getRequesterContact())
                .notes(request.getNotes())
                .status(request.getStatus() != null ? request.getStatus() : VehicleBookingStatus.PLANNED)
                .build();

        VehicleBooking saved = bookingRepository.save(booking);
        log.info("Prenotazione {} creata per il veicolo {}", saved.getId(), vehicle.getPlate());
        return toResponse(saved);
    }

    public VehicleBookingResponse update(Long id, VehicleBookingRequest request) {
        VehicleBooking existing = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prenotazione non trovata: " + id));
        Vehicle vehicle = existing.getVehicle();
        validateBookingData(vehicle, request.getStartDateTime(), request.getEndDateTime(), existing.getId());

        existing.setStartDateTime(request.getStartDateTime());
        existing.setEndDateTime(request.getEndDateTime());
        existing.setTitle(request.getTitle());
        existing.setRequesterName(request.getRequesterName());
        existing.setRequesterContact(request.getRequesterContact());
        existing.setNotes(request.getNotes());
        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        }

        bookingRepository.save(existing);
        return toResponse(existing);
    }

    public void delete(Long id) {
        VehicleBooking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prenotazione non trovata: " + id));
        bookingRepository.delete(booking);
    }

    @Transactional(readOnly = true)
    public long countActive() {
        return bookingRepository.countActive(VehicleBookingStatus.CANCELLED, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public long countForDate(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);
        return bookingRepository.countForDate(VehicleBookingStatus.CANCELLED, start, end);
    }

    @Transactional(readOnly = true)
    public long countUpcomingWithinDays(int days) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime to = now.plusDays(days);
        return bookingRepository.countStartingBetween(VehicleBookingStatus.CANCELLED, now, to);
    }

    private void validateBookingData(Vehicle vehicle, LocalDateTime start, LocalDateTime end, Long excludeBookingId) {
        List<String> errors = new ArrayList<>();
        if (vehicle == null) {
            errors.add("Veicolo obbligatorio");
        }
        if (start == null) {
            errors.add("Data/ora inizio obbligatoria");
        }
        if (end == null) {
            errors.add("Data/ora fine obbligatoria");
        }
        if (start != null && end != null && !end.isAfter(start)) {
            errors.add("La data fine deve essere successiva alla data inizio");
        }
        if (!errors.isEmpty()) {
            throw new BusinessValidationException("Prenotazione non valida", errors);
        }

        VehicleStatus status = vehicle.getStatus();
        if (status == null || status != VehicleStatus.IN_SERVICE) {
            String currentStatus = status != null ? status.getDisplayName() : "Non definito";
            errors.add("Il veicolo non è disponibile per la prenotazione (stato attuale: "
                    + currentStatus + ")");
        }

        checkAssignmentsOverlap(vehicle.getId(), start, end, errors);
        checkOtherBookingsOverlap(vehicle.getId(), start, end, excludeBookingId, errors);

        if (!errors.isEmpty()) {
            throw new BusinessValidationException("Prenotazione non valida", errors);
        }
    }

    private void checkAssignmentsOverlap(Long vehicleId, LocalDateTime start, LocalDateTime end, List<String> errors) {
        List<Assignment> activeAssignments = assignmentRepository.findByVehicleIdAndStatus(vehicleId, AssignmentStatus.ASSIGNED);
        for (Assignment assignment : activeAssignments) {
            LocalDateTime assignmentStart = combine(assignment.getStartDate(), assignment.getStartTime(), LocalTime.MIN);
            LocalDateTime assignmentEnd = combine(assignment.getEndDate(), assignment.getEndTime(), LocalTime.MAX);
            if (assignmentEnd == null) {
                assignmentEnd = FAR_FUTURE_DATE.atTime(LocalTime.MAX);
            }
            if (assignmentStart == null) {
                assignmentStart = assignment.getStartDate() != null
                        ? assignment.getStartDate().atStartOfDay()
                        : LocalDateTime.MIN;
            }
            if (periodsOverlap(start, end, assignmentStart, assignmentEnd)) {
                errors.add("Il veicolo è assegnato nel periodo dal "
                        + formatDateTime(assignmentStart) + " al " + formatDateTime(assignmentEnd));
                break;
            }
        }
    }

    private void checkOtherBookingsOverlap(Long vehicleId, LocalDateTime start, LocalDateTime end,
                                           Long excludeBookingId, List<String> errors) {
        List<VehicleBooking> bookings = bookingRepository.findByVehicleIdOrderByStartDateTimeAsc(vehicleId);
        for (VehicleBooking booking : bookings) {
            if (VehicleBookingStatus.CANCELLED.equals(booking.getStatus())) {
                continue;
            }
            if (excludeBookingId != null && booking.getId().equals(excludeBookingId)) {
                continue;
            }
            if (periodsOverlap(start, end, booking.getStartDateTime(), booking.getEndDateTime())) {
                errors.add("Conflitto con un'altra prenotazione dal "
                        + formatDateTime(booking.getStartDateTime()) + " al "
                        + formatDateTime(booking.getEndDateTime()));
                break;
            }
        }
    }

    private boolean periodsOverlap(LocalDateTime start1, LocalDateTime end1,
                                    LocalDateTime start2, LocalDateTime end2) {
        return start1.isBefore(end2) && end1.isAfter(start2);
    }

    private LocalDateTime combine(LocalDate date, LocalTime time, LocalTime defaultTime) {
        if (date == null) {
            return null;
        }
        LocalTime effectiveTime = time != null ? time : defaultTime;
        return LocalDateTime.of(date, effectiveTime);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? DATE_TIME_FORMATTER.format(dateTime) : "";
    }

    private VehicleBookingResponse toResponse(VehicleBooking booking) {
        Vehicle vehicle = booking.getVehicle();
        String vehicleLabel = vehicle != null
                ? vehicle.getPlate() + " – " + safe(vehicle.getBrand()) + " " + safe(vehicle.getModel())
                : "";
        return VehicleBookingResponse.builder()
                .id(booking.getId())
                .vehicleId(vehicle != null ? vehicle.getId() : null)
                .vehicleLabel(vehicleLabel.trim())
                .title(booking.getTitle())
                .requesterName(booking.getRequesterName())
                .requesterContact(booking.getRequesterContact())
                .status(booking.getStatus())
                .startDateTime(booking.getStartDateTime())
                .endDateTime(booking.getEndDateTime())
                .notes(booking.getNotes())
                .build();
    }

    private String safe(String value) {
        return value != null ? value : "";
    }
}