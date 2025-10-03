package com.veely.service;


import com.veely.entity.Document;
import com.veely.entity.FuelCard;
import com.veely.entity.Vehicle;
import com.veely.exception.ResourceNotFoundException;
import com.veely.model.DocumentType;
import com.veely.model.MileageSource;
import com.veely.model.VehicleStatus;
import com.veely.repository.DocumentRepository;
import com.veely.repository.FuelCardRepository;
import com.veely.repository.VehicleRepository;
import com.veely.repository.AssignmentRepository;
import com.veely.repository.RefuelRepository;
import com.veely.repository.VehicleBookingRepository;
import com.veely.service.FileSystemStorageService;
import com.veely.repository.MaintenanceRepository;
import com.veely.repository.VehicleTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Servizio per la gestione completa dei veicoli,
 * inclusi calcolo totale e upload/download di foto e documenti.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class VehicleService {

    private final VehicleRepository vehicleRepo;
    private final DocumentRepository documentRepo;
    private final FileSystemStorageService fileStorage;
    private final FuelCardRepository fuelCardRepo;
    //private final MaintenanceService maintenanceService;
    private final AssignmentRepository assignmentRepo;
    private final RefuelRepository refuelRepo;
    private final MaintenanceRepository maintenanceRepo;
    private final VehicleTaskRepository taskRepo;
    private final VehicleTaskService vehicleTaskService;
    private final VehicleMileageService mileageService;
    private final VehicleBookingRepository vehicleBookingRepo;

    // ---------------------- CRUD VEICOLO ----------------------

    public Vehicle create(Vehicle payload) {
    	// Se dalla form arriva solo l'id della fuel card, recuperiamo l'entità
        // prima di salvare il veicolo per evitare TransientObjectException
    	log.info("Creazione nuovo veicolo con targa: {}", payload.getPlate());
    	try {
        FuelCard card = null;
        if (payload.getFuelCard() != null && payload.getFuelCard().getId() != null) {
            card = fuelCardRepo.findById(payload.getFuelCard().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Fuel card non trovata: " + payload.getFuelCard().getId()));
        }

        // Rimuoviamo la relazione prima del salvataggio del veicolo per non
        // avere riferimenti a entità non gestite
        payload.setFuelCard(null);
        
        payload.setStatus(VehicleStatus.IN_SERVICE);
        BigDecimal financial = safe(payload.getFinancialFee());
        BigDecimal assistance = safe(payload.getAssistanceFee());
        payload.setTotalFee(financial.add(assistance));
        
        Vehicle saved = vehicleRepo.save(payload);
        if (card != null) {
            card.setVehicle(saved);
            fuelCardRepo.save(card);
            saved.setFuelCard(card);
        }
        
        vehicleTaskService.ensureTasksExist(saved);
        mileageService.recordMileage(saved, saved.getCurrentMileage(), MileageSource.VEHICLE, saved.getId(), LocalDate.now());
        
        return saved;
    	}catch (Exception e) {
            log.error("Errore durante la creazione del veicolo con targa: {}", 
                    payload.getPlate(), e);
            throw e;
        }
    }

    @CacheEvict(value = "vehicleDetails", key = "#id")
    public Vehicle update(Long id, Vehicle payload) {
    	log.info("Aggiornamento veicolo ID: {}", id);
        try {
        Vehicle existing = findByIdOrThrow(id);
        log.debug("Veicolo trovato: {}", existing.getPlate());
        existing.setPlate(payload.getPlate());
        existing.setChassisNumber(payload.getChassisNumber());
        existing.setBrand(payload.getBrand());
        existing.setModel(payload.getModel());
        existing.setSeries(payload.getSeries());
        existing.setYear(payload.getYear());
        existing.setType(payload.getType());
        existing.setFuelType(payload.getFuelType());
        existing.setOwnership(payload.getOwnership());
        existing.setSupplier(payload.getSupplier());
        existing.setRegistrationDate(payload.getRegistrationDate());
        existing.setContractStartDate(payload.getContractStartDate());
        existing.setContractEndDate(payload.getContractEndDate());
        existing.setContractDuration(payload.getContractDuration());
        existing.setContractualKm(payload.getContractualKm());
        existing.setFinancialFee(safe(payload.getFinancialFee()));
        existing.setAssistanceFee(safe(payload.getAssistanceFee()));
        existing.setTotalFee(existing.getFinancialFee().add(existing.getAssistanceFee()));
        existing.setAnnualFringeBenefit(payload.getAnnualFringeBenefit());
        existing.setMonthlyFringeBenefit(payload.getMonthlyFringeBenefit());
        existing.setStatus(payload.getStatus());
        existing.setCurrentMileage(payload.getCurrentMileage());
        if (payload.getFuelCard() != null && payload.getFuelCard().getId() != null) {
            FuelCard card = fuelCardRepo.findById(payload.getFuelCard().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Fuel card non trovata: " + payload.getFuelCard().getId()));
            card.setVehicle(existing);
            fuelCardRepo.save(card);
        }
        existing.setTelepass(payload.getTelepass());
        existing.setInsuranceExpiryDate(payload.getInsuranceExpiryDate());
        existing.setCarTaxExpiryDate(payload.getCarTaxExpiryDate());
        existing.setImagePath(payload.getImagePath());
        Vehicle saved = vehicleRepo.save(existing);
        mileageService.updateMileage(MileageSource.VEHICLE, saved.getId(), saved, saved.getCurrentMileage(), LocalDate.now());
        log.info("Veicolo ID: {} aggiornato con successo", id);
        return saved;
        }catch (Exception e) {
            log.error("Errore durante l'aggiornamento del veicolo ID: {}", id, e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "vehicleDetails", key = "#id")
    public Vehicle findByIdOrThrow(Long id) {
	log.debug("Caricamento veicolo ID: {} (sarà cachato)", id);
        return vehicleRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Veicolo non trovato: " + id));
    }


    @Transactional(readOnly = true)
    public List<Vehicle> findAll() {
        return vehicleRepo.findAll();
    }


    
    /** Veicoli che non sono attualmente assegnati. */
    @Transactional(readOnly = true)
    public List<Vehicle> findAvailable() {
        return vehicleRepo.findByStatusNot(VehicleStatus.ASSIGNED);
    }

    /** Veicoli senza una fuel */
    @Transactional(readOnly = true)
    public List<Vehicle> findWithoutFuelCard() {
        return vehicleRepo.findWithoutFuelCard();
    }
    
    public void delete(Long id) {
    	log.warn("Richiesta eliminazione veicolo ID: {}", id);
    	try {
        Vehicle v = findByIdOrThrow(id);
     // sgancia eventuale fuel card
        FuelCard card = fuelCardRepo.findByVehicleId(id);
        if (card != null) {
            card.setVehicle(null);
            fuelCardRepo.save(card);
        }

        // elimina assegnazioni e documenti collegati
        assignmentRepo.deleteAll(assignmentRepo.findByVehicleId(id));
        documentRepo.deleteAll(documentRepo.findByVehicleId(id));

        // rimuove rifornimenti, manutenzioni e task
        refuelRepo.deleteAll(refuelRepo.findByVehicleId(id));
        maintenanceRepo.deleteAll(maintenanceRepo.findByVehicleId(id));
        taskRepo.deleteAll(taskRepo.findByVehicleId(id));
        vehicleBookingRepo.deleteByVehicleId(id);
        mileageService.deleteByVehicle(id);
        fileStorage.deleteDirectory("vehicles/" + id);
        vehicleRepo.delete(v);
        log.info("Veicolo ID: {} eliminato con successo", id);
    	}catch (Exception e) {
            log.error("Errore durante l'eliminazione del veicolo ID: {}", id, e);
            throw e;
        }
    }
    
    /**
     * Metodo ottimizzato per lista veicoli
     */
    @Transactional(readOnly = true)
    public List<Vehicle> findAllOptimized() {
        log.debug("Caricamento lista veicoli ottimizzata");
        
        // Prima query: veicoli con supplier e fuel card
        List<Vehicle> vehicles = vehicleRepo.findAllWithRelations();
        
        // Seconda query: carica assignments per tutti i veicoli in batch
        if (!vehicles.isEmpty()) {
            vehicleRepo.loadAssignmentsForVehicles(vehicles);
        }
        
        return vehicles;
    }
    


    // ---------------------- FOTO VEICOLO ----------------------

    /**
     * Carica una fotografia del veicolo in uploads/vehicles/{id}/photos.
     * Registra un Document con type = VEHICLE_IMAGE.
     */
    public Document uploadPhoto(Long vehicleId, MultipartFile file) {
        Vehicle v = findByIdOrThrow(vehicleId);
        String subDir = "vehicles/" + vehicleId + "/photos";
        String filename = fileStorage.store(file, subDir);

        Document doc = Document.builder()
                .vehicle(v)
                .type(DocumentType.VEHICLE_IMAGE)
                .path(subDir + "/" + filename)
                .issueDate(LocalDate.now())
                .expiryDate(null)
                .build();
        return documentRepo.save(doc);
    }

    @Transactional(readOnly = true)
    public Resource loadPhoto(Long vehicleId, String filename) {
        String subDir = "vehicles/" + vehicleId + "/photos";
        return fileStorage.loadAsResource(filename, subDir);
    }

    // ---------------------- DOCUMENTI VEICOLO ----------------------

    /**
     * Carica un documento generico (leasing, assicurazione, manutenzione…)
     * in uploads/vehicles/{id}/docs.
     */
    public Document uploadDocument(Long vehicleId,
                                   MultipartFile file,
                                   DocumentType type,
                                   LocalDate issueDate,
                                   LocalDate expiryDate) {
        Vehicle v = findByIdOrThrow(vehicleId);
        String subDir = "vehicles/" + vehicleId + "/docs";
        String filename = fileStorage.store(file, subDir);

        Document doc = Document.builder()
                .vehicle(v)
                .type(type)
                .path(subDir + "/" + filename)
                .issueDate(issueDate)
                .expiryDate(expiryDate)
                .build();
        return documentRepo.save(doc);
    }

    @Transactional(readOnly = true)
    public Resource loadDocument(Long vehicleId, String filename) {
        String subDir = "vehicles/" + vehicleId + "/docs";
        return fileStorage.loadAsResource(filename, subDir);
    }

    /**
     * Elimina un documento veicolo dal DB e dal filesystem.
     */
    public void deleteDocument(Long docId) {
        Document doc = documentRepo.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento non trovato: " + docId));

        String fullPath = doc.getPath(); // es. "vehicles/12/docs/123_filename.pdf"
        int lastSlash = fullPath.lastIndexOf('/');
        String subDir = fullPath.substring(0, lastSlash);
        String filename = fullPath.substring(lastSlash + 1);

        fileStorage.delete(filename, subDir);
        documentRepo.delete(doc);
    }
    
    /**
     * Aggiorna il chilometraggio corrente del veicolo.
     *
     * @param vehicleId  id del veicolo da aggiornare
     * @param mileage    nuovo valore dei km percorsi
     * @return entità Vehicle aggiornata
     */
    @CacheEvict(value = "vehicleDetails", key = "#vehicleId")
    public Vehicle updateMileage(Long vehicleId, int mileage) {
        Vehicle vehicle = findByIdOrThrow(vehicleId);
        Integer current = vehicle.getCurrentMileage();
        if (current != null && mileage < current) {
            throw new IllegalArgumentException("Il kilometraggio non può essere inferiore al precedente");
        }
        vehicle.setCurrentMileage(mileage);
        mileageService.updateMileage(MileageSource.VEHICLE, vehicleId, vehicle, mileage, LocalDate.now());
        return vehicleRepo.save(vehicle);
    }

    // ---------------------- UTILI INTERNI ----------------------

    private BigDecimal safe(BigDecimal x) {
        return (x != null) ? x : BigDecimal.ZERO;
    }
    
    /** Aggiorna la data di scadenza assicurazione. */
    public void updateInsuranceExpiry(Long vehicleId, LocalDate newDate) {
        Vehicle v = findByIdOrThrow(vehicleId);
        v.setInsuranceExpiryDate(newDate);
        vehicleRepo.save(v);
    }

    /** Aggiorna la data di scadenza bollo. */
    public void updateCarTaxExpiry(Long vehicleId, LocalDate newDate) {
        Vehicle v = findByIdOrThrow(vehicleId);
        v.setCarTaxExpiryDate(newDate);
        vehicleRepo.save(v);
    }

    /** Aggiorna la data di scadenza della fuel card. */
    public void updateFuelCardExpiry(Long vehicleId, LocalDate newDate) {
    	 FuelCard card = fuelCardRepo.findByVehicleId(vehicleId);
        if (card != null) {
            card.setExpiryDate(newDate);
            fuelCardRepo.save(card);
        }
    }
    /** Aggiorna la scadenza del contratto di leasing. */
    public void updateLeaseExpiry(Long vehicleId, LocalDate newDate) {
        Vehicle v = findByIdOrThrow(vehicleId);
        v.setContractEndDate(newDate);
        vehicleRepo.save(v);
    }
}