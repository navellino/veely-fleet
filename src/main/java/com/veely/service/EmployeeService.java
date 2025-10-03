package com.veely.service;

import com.veely.entity.Employee;
import com.veely.entity.ExpenseReport;
import com.veely.entity.FuelCard;
import com.veely.entity.Document;
import com.veely.exception.ResourceNotFoundException;
import com.veely.model.EducationLevel;
import com.veely.model.EmploymentStatus;
import com.veely.model.MaritalStatus;
import com.veely.repository.DocumentRepository;
import com.veely.repository.EmployeeRepository;
import com.veely.repository.FuelCardRepository;
import com.veely.repository.ComplianceItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.io.IOException;

/**
 * Servizio per la gestione CRUD di Employee, ricerca/filtro/paginazione,
 * e pulizia dei documenti personali dal DB e filesystem.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeService {

    private final EmployeeRepository employeeRepo;
    private final DocumentRepository documentRepo;
    private final FileSystemStorageService fileStorage;
    private final PasswordEncoder passwordEncoder;
    private final PdfEmployeeService pdfEmployeeService;
    private final ComplianceItemRepository complianceItemRepo;
    private final FuelCardRepository fuelCardRepo;
    private final ExpenseReportService expenseReportService;
    
    /**
     * Crea un nuovo dipendente.
     */
    public Employee create(Employee employee) {
    	validateBusinessRules(employee);
        if (StringUtils.hasText(employee.getPassword())) {
            employee.setPassword(passwordEncoder.encode(employee.getPassword()));
        }
        return employeeRepo.save(employee);
    }
    
 // restituisce tutti i valori dell’enum, ordinati per displayName
    public List<MaritalStatus> listMaritalStatuses() {
      return Arrays.stream(MaritalStatus.values())
                   .sorted(Comparator.comparing(MaritalStatus::getDisplayName))
                   .toList();
    }

    public List<EducationLevel> listEducationLevels() {
      return Arrays.stream(EducationLevel.values())
                   .sorted(Comparator.comparing(EducationLevel::getDisplayName))
                   .toList();
    }
    
    /**
     * Aggiornamento del dipendente
     **/

    @Transactional
    @CacheEvict(value = "employeeDetails", key = "#id")
    public Employee update(Long id, Employee payload) {
    	log.info("Aggiornamento dipendente ID: {} (invalida cache)", id);
        // 1) carico l’esistente
        Employee existing = findByIdOrThrow(id);

        // 2) sovrascrivo solo i campi anagrafici
        existing.setFirstName(payload.getFirstName());
        existing.setLastName(payload.getLastName());
        existing.setBirthDate(payload.getBirthDate());
        existing.setBirthPlace(payload.getBirthPlace());
        existing.setGender(payload.getGender());
        existing.setFiscalCode(payload.getFiscalCode());
        existing.setIban(payload.getIban());
        
     // 2) Contatti e ruolo
        existing.setEmail(payload.getEmail());
        existing.setPhone(payload.getPhone());
        existing.setMobile(payload.getMobile());
        existing.setPec(payload.getPec());
        existing.setRoles(payload.getRoles());

        // 3) password: se ne è stata fornita una nuova, la codifico e la setto
        String newPwd = payload.getPassword();
        if (newPwd != null && !newPwd.isBlank()) {
            existing.setPassword(passwordEncoder.encode(newPwd));
        }
        // altrimenti lasciamo inalterata quella già in DB
        
     // 3) Stati civili e titoli di studio
        existing.setMaritalStatus(payload.getMaritalStatus());
        existing.setEducationLevel(payload.getEducationLevel());
        
     // 4) Indirizzo di residenza (embed)
        existing.setResidenceAddress(payload.getResidenceAddress());

        // 4) salvo e rendo persistente
        return employeeRepo.save(existing);
    }

    /**
     * Restituisce il dipendente per ID o lancia eccezione se non esiste.
     */
    @Transactional(readOnly = true)
    public Employee findByIdOrThrow(Long id) {
        return employeeRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dipendente non trovato: " + id));
    }

    /**
     * Elenca tutti i dipendenti (senza paginazione).
     */
    @Transactional(readOnly = true)
    public List<Employee> findAll() {
        return employeeRepo.findAll();
    }
    
    /** Trova un dipendente a partire dalla sua email. */
    @Transactional(readOnly = true)
    public Employee findByEmail(String email) {
        return employeeRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Dipendente non trovato: " + email));
    }
    
    /**
     * Esporta l'elenco dei dipendenti in formato PDF.
     */
    @Transactional(readOnly = true)
    public byte[] exportStyledPdf() throws IOException {
        List<Employee> employees = employeeRepo.findAll();
        return pdfEmployeeService.exportStyledPdf(employees);
    }

    /** Restituisce i dipendenti senza un rapporto di lavoro attivo */
    @Transactional(readOnly = true)
    public List<Employee> findAvailableForEmployment() {
        return employeeRepo.findAvailableForEmployment(EmploymentStatus.ACTIVE);
    }
    
    /** Dipendenti senza una fuel card attiva */
    @Transactional(readOnly = true)
    public List<Employee> findWithoutFuelCard() {
        return employeeRepo.findWithoutFuelCard();
    }
    
    /**
     * Elenca i dipendenti con paginazione e carica anche i rapporti di lavoro
     * per evitare query N+1
     */
    @Transactional(readOnly = true)
    public Page<Employee> findAllWithEmployments(Pageable pageable) {
        log.debug("Caricamento dipendenti con employments - pagina: {}", pageable.getPageNumber());
        return employeeRepo.findAllWithEmployments(pageable);
    }
    
    /**
     * Restituisce il dipendente per ID con tutte le relazioni caricate
     * per evitare lazy loading exceptions
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "employeeDetails", key = "#id")
    public Employee findByIdWithRelations(Long id) {
        log.debug("Caricamento dipendente ID: {} con relazioni (sarà cachato)", id);
        return employeeRepo.findByIdWithAllRelations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dipendente non trovato: " + id));
    }

    /**
     * Ricerca per nome o cognome contenente la parola chiave, paginata.
     */
    @Transactional(readOnly = true)
    public Page<Employee> search(String keyword, Pageable pageable) {
        String like = "%" + keyword.trim().toLowerCase() + "%";
        return employeeRepo.findByFirstNameIgnoreCaseContainingOrLastNameIgnoreCaseContaining(like, like, pageable);
    }

    /**
     * Elimina un dipendente e pulisce i suoi documenti personali dal DB e dal filesystem.
     */
    @CacheEvict(value = "employeeDetails", key = "#id")
    public void delete(Long id) {
    	log.warn("Eliminazione dipendente ID: {} (invalida cache)", id);
        Employee e = findByIdOrThrow(id);
        
     // sgancia eventuale fuel card
        FuelCard card = fuelCardRepo.findByEmployeeId(id);
        if (card != null) {
            card.setEmployee(null);
            fuelCardRepo.save(card);
        }
        
        // Rimuovo record Document associati all'employee
        List<Document> docs = documentRepo.findByEmployeeId(id);
        /*
       
        docs.forEach(doc -> {
            String fullPath = doc.getPath();
            int sep = fullPath.lastIndexOf('/');
            String subDir = sep > 0 ? fullPath.substring(0, sep) : "";
            String filename = sep > 0 ? fullPath.substring(sep + 1) : fullPath;
            fileStorage.delete(filename, subDir);
        });
        documentRepo.deleteAll(docs);*/
        
        docs.forEach(doc -> fileStorage.delete(doc.getPath()));
        documentRepo.deleteAll(docs);
        e.getPersonalDocuments().clear();
        
        e.getPersonalDocuments().forEach(doc -> fileStorage.delete(doc.getPath()));
        // Rimuovo directory fisica
        fileStorage.deleteDirectory("employees/" + id + "/docs");
     // Rimuovo eventuali note spese associate
        List<ExpenseReport> reports = expenseReportService.findByEmployeeId(id);
        reports.forEach(r -> expenseReportService.delete(r.getId()));        
        // Rimuovo eventuali compliance item associati
        complianceItemRepo.deleteByEmployeeId(id);
        // Cancello l'entity
        employeeRepo.delete(e);
    }
    
    public Resource loadEmployeeDocumentAsResource(Long employeeId, String filename) {
        // es. delega a FileSystemStorageService
        return fileStorage.loadAsResource("employee/" + employeeId + "/docs/", filename);
    }
    
    /**
     * Carica i documenti per una lista di dipendenti in batch
     * per evitare query N+1
     */
    @Transactional(readOnly = true)
    public void loadDocumentsForEmployees(List<Employee> employees) {
        if (!employees.isEmpty()) {
            log.debug("Caricamento documenti in batch per {} dipendenti", employees.size());
            employeeRepo.loadDocumentsForEmployees(employees);
        }
    }
    
 // AGGIUNGI questo metodo per validazioni business
    private void validateBusinessRules(Employee employee) {
        // Log per dipendenti minorenni
        if (employee.getAge() != null && employee.getAge() < 18) {
            log.warn("Dipendente minorenne registrato: {} {} (età: {} anni)", 
                employee.getFirstName(), employee.getLastName(), employee.getAge());
        }
        
        // Altre validazioni business se necessarie
        if (employee.getBirthDate() != null && 
            employee.getBirthDate().isAfter(java.time.LocalDate.now().minusYears(16))) {
            throw new IllegalArgumentException("Il dipendente deve avere almeno 16 anni");
        }
    }
    
    // AGGIUNGI questi metodi per verificare duplicati
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return employeeRepo.existsByEmail(email);
    }
    
    @Transactional(readOnly = true)
    public boolean existsByFiscalCode(String fiscalCode) {
        return employeeRepo.existsByFiscalCode(fiscalCode);
    }
        
}