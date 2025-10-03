package com.veely.service;

import com.veely.entity.AdminDocument;
import com.veely.entity.Assignment;
import com.veely.entity.CompanyInfo;
import com.veely.entity.ComplianceItem;
import com.veely.entity.Contract;
import com.veely.entity.Correspondence;
import com.veely.entity.Document;
import com.veely.entity.Employee;
import com.veely.entity.Employment;
import com.veely.entity.ExpenseItem;
import com.veely.entity.Insurance;
import com.veely.entity.Vehicle;
import com.veely.entity.Maintenance;
import com.veely.entity.Project;
import com.veely.entity.Supplier;
import com.veely.service.ProjectService;
import com.veely.repository.EmployeeRepository;
import com.veely.repository.EmploymentRepository;
import com.veely.repository.MaintenanceRepository;
import com.veely.repository.SupplierRepository;
import com.veely.exception.ResourceNotFoundException;
import com.veely.model.DocumentType;
import com.veely.repository.AdminDocumentRepository;
import com.veely.repository.CompanyInfoRepository;
import com.veely.repository.DocumentRepository;
import com.veely.repository.InsuranceRepository;
import com.veely.repository.ExpenseItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DocumentService {

    private final DocumentRepository documentRepo;
    private final FileSystemStorageService fileStorage;
    private final EmployeeRepository employeeRepo;
    private final EmploymentRepository employmentRepo;
    private final VehicleService vehicleService;
    private final AssignmentService assignmentService;
    private final CorrespondenceService correspondenceService;
    private final ExpenseItemRepository itemRepo;
    private final MaintenanceRepository maintenanceRepo;
    private final ComplianceItemService complianceItemService;
    private final CompanyInfoRepository companyInfoRepository;
    private final ProjectService projectService;
    private final ContractService contractService;
    private final SupplierRepository supplierRepo;
    private final InsuranceRepository insuranceRepository;
    private final AdminDocumentRepository adminDocumentRepository;

    /**
     * Salva un logo aziendale utilizzando lo stesso meccanismo di storage
     * dei documenti. Ritorna il percorso relativo del file salvato.
     */
    public String storeCompanyLogo(MultipartFile file) {
        String subdir = "company/logos";
        fileStorage.initDirectory(subdir);
        String filename = fileStorage.store(file, subdir);
        return subdir + "/" + filename;
    }

    /**
     * Elimina un file dato il percorso relativo nella cartella uploads.
     */
    public void deleteFile(String relativePath) {
        if (relativePath != null && !relativePath.isBlank()) {
            fileStorage.delete(relativePath);
        }
    }

    
    /** Recupera un documento o lancia eccezione */
    @Transactional(readOnly = true)
    public Document getDocument(Long documentId) {
        return documentRepo.findById(documentId)
            .orElseThrow(() -> new ResourceNotFoundException("Documento non trovato: " + documentId));
    }

    /** Elimina file e record DB */
    public void deleteDocument(Long documentId) throws IOException {
        Document doc = documentRepo.findById(documentId)
            .orElseThrow(() -> new ResourceNotFoundException("Documento non trovato: " + documentId));
        Path p = Path.of(doc.getPath());
        String subdir = p.getParent().toString();
        String fn    = p.getFileName().toString();
        fileStorage.delete(fn, subdir);
        documentRepo.delete(doc);
    }

    /** Carica risorsa per download */
    @Transactional(readOnly = true)
    public Resource loadDocument(Long documentId) {
        Document doc = getDocument(documentId);
        Path p = Path.of(doc.getPath());
        String subdir = p.getParent().toString();
        String filename = p.getFileName().toString();
        return fileStorage.loadAsResource(filename, subdir);
    }

    /** Restituisce tutti i documenti di un dipendente */
    @Transactional(readOnly = true)
    public List<Document> getEmployeeDocuments(Long employeeId) {
        // derivato da Document.employee.id
        return documentRepo.findByEmployeeId(employeeId);
    }

    // --- Employment Documents ---
    @Transactional(readOnly = true)
    public List<Document> getEmploymentDocuments(Long employmentId) {
        return documentRepo.findByEmploymentId(employmentId);
    }

    public Document uploadEmploymentDocument(Long employmentId,
                                             MultipartFile file,
                                             DocumentType type,
                                             LocalDate issueDate,
                                             LocalDate expiryDate) throws IOException {
	Employment empmt = employmentRepo.findById(employmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Rapporto di lavoro non trovato: " + employmentId));
        //String subdir = "employments/" + employmentId + "/docs";
        String subdir = "employments/" + empmt.getMatricola() + "/docs";
        fileStorage.initDirectory(subdir);
        String filename = fileStorage.store(file, subdir);

        Document doc = Document.builder()
            .employment(empmt)
            .type(type)
            .issueDate(issueDate)
            .expiryDate(expiryDate)
            .path(subdir + "/" + filename)
            .build();
        return documentRepo.save(doc);
    }

    // --- Vehicle Documents ---
    @Transactional(readOnly = true)
    public List<Document> getVehicleDocuments(Long vehicleId) {
        return documentRepo.findByVehicleId(vehicleId);
    }

    public Document uploadVehicleDocument(Long vehicleId,
                                          MultipartFile file,
                                          DocumentType type,
                                          LocalDate issueDate,
                                          LocalDate expiryDate) throws IOException {
        Vehicle veh = vehicleService.findByIdOrThrow(vehicleId);
        String subdir = "vehicles/" + vehicleId + "/docs";
        fileStorage.initDirectory(subdir);
        String filename = fileStorage.store(file, subdir);

        Document doc = Document.builder()
            .vehicle(veh)
            .type(type)
            .issueDate(issueDate)
            .expiryDate(expiryDate)
            .path(subdir + "/" + filename)
            .build();
        return documentRepo.save(doc);
    }

    // --- Assignment Documents ---
    @Transactional(readOnly = true)
    public List<Document> getAssignmentDocuments(Long assignmentId) {
        return documentRepo.findByAssignmentId(assignmentId);
    }

    public Document uploadAssignmentDocument(Long assignmentId,
                                             MultipartFile file,
                                             DocumentType type,
                                             LocalDate issueDate,
                                             LocalDate expiryDate) throws IOException {
        Assignment asg = assignmentService.findByIdOrThrow(assignmentId);
        String subdir = "assignments/" + assignmentId + "/docs";
        fileStorage.initDirectory(subdir);
        String filename = fileStorage.store(file, subdir);

        Document doc = Document.builder()
            .assignment(asg)
            .type(type)
            .issueDate(issueDate)
            .expiryDate(expiryDate)
            .path(subdir + "/" + filename)
            .build();
        return documentRepo.save(doc);
    }
    
    // --- Project Documents ---
    @Transactional(readOnly = true)
    public List<Document> getProjectDocuments(Long projectId) {
        return documentRepo.findByProjectId(projectId);
    }

    public Document uploadProjectDocument(Long projectId,
                                          MultipartFile file,
                                          DocumentType type,
                                          LocalDate issueDate,
                                          LocalDate expiryDate) throws IOException {
        Project project = projectService.findByIdOrThrow(projectId);
        String subdir = "projects/" + projectId + "/docs";
        fileStorage.initDirectory(subdir);
        String filename = fileStorage.store(file, subdir);

        Document doc = Document.builder()
            .project(project)
            .type(type)
            .issueDate(issueDate)
            .expiryDate(expiryDate)
            .path(subdir + "/" + filename)
            .build();
        return documentRepo.save(doc);
    }

    @Transactional(readOnly = true)
    public Resource loadProjectDocumentAsResource(Long projectId, String filename) {
        String dir = "projects/" + projectId + "/docs";
        return fileStorage.loadAsResource(filename, dir);
    }

 // --- Project Insurance Documents ---
    @Transactional(readOnly = true)
    public List<Document> getInsuranceDocuments(Long insuranceId) {
        return documentRepo.findByInsuranceId(insuranceId);
    }

    public Document uploadInsuranceDocument(Long insuranceId,
            MultipartFile file,
            DocumentType type,
            LocalDate issueDate,
            LocalDate expiryDate) throws IOException {
		Insurance insurance = insuranceRepository.findById(insuranceId)
		.orElseThrow(() -> new ResourceNotFoundException("Polizza non trovata: " + insuranceId));
		
		String subdir;
		if (insurance.getProject() != null && insurance.getProject().getId() != null) {
		subdir = "projects/" + insurance.getProject().getId() + "/policies/" + insuranceId;
		} else {
		subdir = "policies/" + insuranceId;
		}
        fileStorage.initDirectory(subdir);
        String filename = fileStorage.store(file, subdir);

        Document doc = Document.builder()
        		.insurance(insurance)
                .type(type)
                .issueDate(issueDate)
                .expiryDate(expiryDate)
                .path(subdir + "/" + filename)
                .build();
        return documentRepo.save(doc);
    }

    @Transactional(readOnly = true)
    public List<com.veely.model.DocumentInfo> getInsuranceDocumentInfo(Long insuranceId) {
        return documentRepo.findByInsuranceId(insuranceId)
                .stream()
                .map(d -> new com.veely.model.DocumentInfo(d.getId(), d.getPath()))
                .collect(Collectors.toList());
    }

    
 // --- Contract Documents ---
    @Transactional(readOnly = true)
    public List<Document> getContractDocuments(Long contractId) {
        return documentRepo.findByContractId(contractId);
    }

    public Document uploadContractDocument(Long contractId,
                                           MultipartFile file,
                                           DocumentType type,
                                           LocalDate issueDate,
                                           LocalDate expiryDate) throws IOException {
        Contract contract = contractService.findByIdOrThrow(contractId);
        String subdir = "contracts/" + contractId + "/docs";
        fileStorage.initDirectory(subdir);
        String filename = fileStorage.store(file, subdir);

        Document doc = Document.builder()
            .contract(contract)
            .type(type)
            .issueDate(issueDate)
            .expiryDate(expiryDate)
            .path(subdir + "/" + filename)
            .build();
        return documentRepo.save(doc);
    }

    @Transactional(readOnly = true)
    public Resource loadContractDocumentAsResource(Long contractId, String filename) {
        String dir = "contracts/" + contractId + "/docs";
        return fileStorage.loadAsResource(filename, dir);
    }    
    
 // --- Supplier Documents ---
    @Transactional(readOnly = true)
    public List<Document> getSupplierDocuments(Long supplierId) {
        return documentRepo.findBySupplierId(supplierId);
    }

    public Document uploadSupplierDocument(Long supplierId,
                                           MultipartFile file,
                                           DocumentType type,
                                           LocalDate issueDate,
                                           LocalDate expiryDate) throws IOException {
        Supplier supplier = supplierRepo.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Fornitore non trovato: " + supplierId));
        String subdir = "suppliers/" + supplierId + "/docs";
        fileStorage.initDirectory(subdir);
        String filename = fileStorage.store(file, subdir);

        Document doc = Document.builder()
                .supplier(supplier)
                .type(type)
                .issueDate(issueDate)
                .expiryDate(expiryDate)
                .path(subdir + "/" + filename)
                .build();
        return documentRepo.save(doc);
    }

    @Transactional(readOnly = true)
    public Resource loadSupplierDocumentAsResource(Long supplierId, String filename) {
        String dir = "suppliers/" + supplierId + "/docs";
        return fileStorage.loadAsResource(filename, dir);
    }
    
 // --- Administrative Document Files ---
    @Transactional(readOnly = true)
    public List<Document> getAdminDocumentDocuments(Long adminDocumentId) {
        return documentRepo.findByAdminDocumentId(adminDocumentId);
    }

    public Document uploadAdminDocumentDocument(Long adminDocumentId,
    		 MultipartFile file,
             DocumentType type,
             LocalDate issueDate,
             LocalDate expiryDate) throws IOException {
        AdminDocument adminDocument = adminDocumentRepository.findById(adminDocumentId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento amministrativo non trovato: " + adminDocumentId));
        String subdir = "admin_documents/" + adminDocumentId + "/docs";
        fileStorage.initDirectory(subdir);
        String filename = fileStorage.store(file, subdir);

        Document doc = Document.builder()
                .adminDocument(adminDocument)
                .type(DocumentType.OTHER)
                .type(type)
                .issueDate(issueDate)
                .expiryDate(expiryDate)
                .path(subdir + "/" + filename)
                .build();
        return documentRepo.save(doc);
    }

    @Transactional(readOnly = true)
    public Resource loadAdminDocumentDocumentAsResource(Long adminDocumentId, String filename) {
        String dir = "admin_documents/" + adminDocumentId + "/docs";
        return fileStorage.loadAsResource(filename, dir);
    }
    
 // --- Expense Item Documents ---
    @Transactional(readOnly = true)
    public List<Document> getExpenseItemDocuments(Long itemId) {
        return documentRepo.findByExpenseItemId(itemId);
    }

    public Document uploadExpenseItemDocument(Long itemId,
                                              MultipartFile file,
                                              DocumentType type,
                                              LocalDate issueDate,
                                              LocalDate expiryDate) throws IOException {
    	ExpenseItem item = itemRepo.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Voce spesa non trovata: " + itemId));
        String subdir = "expense_items/" + itemId + "/docs";
        fileStorage.initDirectory(subdir);
        String filename = fileStorage.store(file, subdir);

        Document doc = Document.builder()
            .expenseItem(item)
            .type(type)
            .issueDate(issueDate)
            .expiryDate(expiryDate)
            .path(subdir + "/" + filename)
            .build();
        return documentRepo.save(doc);
    }
    
    
 // --- Maintenance Documents ---
    @Transactional(readOnly = true)
    public List<Document> getMaintenanceDocuments(Long maintenanceId) {
        return documentRepo.findByMaintenanceId(maintenanceId);
    }

    public Document uploadMaintenanceDocument(Long maintenanceId,
                                              MultipartFile file,
                                              DocumentType type,
                                              LocalDate issueDate,
                                              LocalDate expiryDate) throws IOException {
        com.veely.entity.Maintenance m = maintenanceRepo.findById(maintenanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Manutenzione non trovata: " + maintenanceId));
        Long vehicleId = m.getVehicle().getId();
        String subdir = "vehicles/" + vehicleId + "/docs";
        fileStorage.initDirectory(subdir);
        String filename = fileStorage.store(file, subdir);

        Document doc = Document.builder()
                .maintenance(m)
                .vehicle(m.getVehicle())
                .type(type)
                .issueDate(issueDate)
                .expiryDate(expiryDate)
                .path(subdir + "/" + filename)
                .build();
        return documentRepo.save(doc);
    }
    
    /** Carica la risorsa per un documento di manutenzione */
    @Transactional(readOnly = true)
    public Resource loadMaintenanceDocumentAsResource(Long maintenanceId, String filename) {
    	Maintenance m = maintenanceRepo.findById(maintenanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Manutenzione non trovata: " + maintenanceId));
    	Long vehicleId = m.getVehicle().getId();
        String dir = "vehicles/" + vehicleId + "/docs";
        return fileStorage.loadAsResource(filename, dir);
    }
    
    
    /**
     * Returns lightweight document info for an expense item to avoid
     * serialization loops when rendering views.
     */
    @Transactional(readOnly = true)
    public List<com.veely.model.DocumentInfo> getExpenseItemDocumentInfo(Long itemId) {
        return documentRepo.findByExpenseItemId(itemId)
                .stream()
                .map(d -> new com.veely.model.DocumentInfo(d.getId(), d.getPath()))
                .toList();
    }
    
    /**
     * Elimina tutti i documenti collegati a una voce di spesa, inclusi i file su disco.
     */
    public void deleteExpenseItemDocuments(Long itemId) {
        List<Document> docs = documentRepo.findByExpenseItemId(itemId);
        for (Document doc : docs) {
            Path p = Path.of(doc.getPath());
            fileStorage.delete(p.getFileName().toString(), p.getParent().toString());
            documentRepo.delete(doc);
        }
        fileStorage.deleteDirectory("expense_items/" + itemId + "/docs");
    }
    
    /**
     * Carica e salva un documento per un dipendente.
     */
    /** Carica fisicamente e salva in DB un documento per un dipendente */
    public Document uploadEmployeeDocument(Long employeeId,
                                           MultipartFile file,
                                           DocumentType type,
                                           LocalDate issueDate,
                                           LocalDate expiryDate) throws IOException {
        // 1. Recupera l’employee o lancia eccezione
    	Employee emp = employeeRepo.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dipendente non trovato: " + employeeId));
        // 2. Prepara la directory e salva il file
        String dir = "employees/" + employeeId + "/docs";
        fileStorage.initDirectory(dir);
        String filename = fileStorage.store(file, dir);
        // 3. Costruisci il Document con il builder corretto
        Document doc = Document.builder()
            .employee(emp)
            .type(type)
            .path(dir + "/" + filename)
            .issueDate(issueDate)
            .expiryDate(expiryDate)
            .build();
        return documentRepo.save(doc);
    }

    /** Carica la risorsa Spring Resource per il download di un documento dipendente */
    @Transactional(readOnly = true)
    public Resource loadEmployeeDocumentAsResource(Long employeeId, String filename) {
        String dir = "employees/" + employeeId + "/docs";
        return fileStorage.loadAsResource(filename, dir);
    }
    
    /** Carica la risorsa per un documento di rapporto di lavoro */
    @Transactional(readOnly = true)
    public Resource loadEmploymentDocumentAsResource(Long employmentId, String filename) {
	Employment emp = employmentRepo.findById(employmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Rapporto di lavoro non trovato: " + employmentId));
        String dir = "employments/" + emp.getMatricola() + "/docs";
        return fileStorage.loadAsResource(filename, dir);
    }
    
    @Transactional
    public void deleteEmployeeDocument(Long empId, Long docId) {
        Document doc = documentRepo.findById(docId).get();
        // cancello il file dal file system
        fileStorage.delete(doc.getPath());
        // cancello il record
        documentRepo.delete(doc);
    }
    
    /** Carica la risorsa per un documento di assegnazione */
    @Transactional(readOnly = true)
    public Resource loadAssignmentDocumentAsResource(Long assignmentId, String filename) {
        assignmentService.findByIdOrThrow(assignmentId);
        String dir = "assignments/" + assignmentId + "/docs";
        return fileStorage.loadAsResource(filename, dir);
    }
    
    // --- Correspondence Documents ---
    @Transactional(readOnly = true)
    public List<Document> getCorrespondenceDocuments(Long correspondenceId) {
        return documentRepo.findByCorrespondenceId(correspondenceId);
    }

    public Document uploadCorrespondenceDocument(Long correspondenceId,
                                                 MultipartFile file,
                                                 DocumentType type,
                                                 LocalDate issueDate,
                                                 LocalDate expiryDate) throws IOException {
        Correspondence corr = correspondenceService.findByIdOrThrow(correspondenceId);
        
        
        String subdir = "correspondence/" + correspondenceId + "/docs";
        fileStorage.initDirectory(subdir);
        String filename = fileStorage.store(file, subdir);

        Document doc = Document.builder()
            .correspondence(corr)
            .type(type)
            .issueDate(issueDate)
            .expiryDate(expiryDate)
            .path(subdir + "/" + filename)
            .build();
        return documentRepo.save(doc);
    }
    
 // --- Compliance Item Documents ---
    @Transactional(readOnly = true)
    public List<Document> getComplianceItemDocuments(Long itemId) {
        return documentRepo.findByComplianceItemId(itemId);
    }
    
    @Transactional(readOnly = true)
    public List<Document> getComplianceItemDocuments(Long itemId, DocumentType type) {
        return documentRepo.findByComplianceItemIdAndType(itemId, type);
    }

    public Document uploadComplianceItemDocument(Long itemId,
                                                 MultipartFile file,
                                                 DocumentType type,
                                                 LocalDate issueDate,
                                                 LocalDate expiryDate) throws IOException {
        ComplianceItem item = complianceItemService.findByIdOrThrow(itemId);
        Employee emp = item.getEmployee();
        Project proj = item.getProject();

        String subdir;
        if (emp != null) {
            subdir = "employees/" + emp.getId() + "/docs";
        } else if (proj != null) {
            subdir = "projects/" + proj.getId() + "/docs";
        } else {
            subdir = "compliance/" + itemId + "/docs";
        }
        fileStorage.initDirectory(subdir);
        String filename = fileStorage.store(file, subdir);

        Document doc = Document.builder()
                .complianceItem(item)
                .employee(emp)
                .project(proj)
                .type(type)
                .issueDate(issueDate)
                .expiryDate(expiryDate)
                .path(subdir + "/" + filename)
                .build();
        return documentRepo.save(doc);
    }
    
    @Transactional(readOnly = true)
    public Resource loadComplianceItemDocumentAsResource(Long itemId, String filename) {
        ComplianceItem item = complianceItemService.findByIdOrThrow(itemId);
        Employee emp = item.getEmployee();
        com.veely.entity.Project proj = item.getProject();

        String dir;
        if (emp != null) {
            dir = "employees/" + emp.getId() + "/docs";
        } else if (proj != null) {
            dir = "projects/" + proj.getId() + "/docs";
        } else {
            dir = "compliance/" + itemId + "/docs";
        }
        return fileStorage.loadAsResource(filename, dir);
    }
    
    /** Restituisce la foto profilo del dipendente, se presente */
    @Transactional(readOnly = true)
    public Document getEmployeeProfilePhoto(Long employeeId) {
        return documentRepo
                .findByEmployeeIdAndType(employeeId, DocumentType.IDENTITY_PHOTO)
                .stream()
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Recupera le foto profilo per una lista di dipendenti in batch
     * per evitare query N+1
     */
    @Transactional(readOnly = true)
    public Map<Long, Document> getProfilePhotosBatch(List<Long> employeeIds) {
        if (employeeIds.isEmpty()) {
            return new HashMap<>();
        }
        
        log.debug("Caricamento foto profilo in batch per {} dipendenti", employeeIds.size());
        
        List<Document> photos = documentRepo.findByEmployeeIdInAndType(
            employeeIds, DocumentType.IDENTITY_PHOTO
        );
        
        return photos.stream()
            .collect(Collectors.toMap(
                doc -> doc.getEmployee().getId(),
                doc -> doc,
                (existing, replacement) -> existing // in caso di duplicati, prendi il primo
            ));
    }
        
    /**
     * Carica un documento per l'azienda (logo, documenti aziendali)
     */
    public Document uploadCompanyDocument(Long companyId,
                                         MultipartFile file,
                                         DocumentType type,
                                         LocalDate issueDate,
                                         LocalDate expiryDate) throws IOException {
        String subdir = "company/" + companyId + "/docs";
        fileStorage.initDirectory(subdir);
        String filename = fileStorage.store(file, subdir);

        Document doc = Document.builder()
            .type(type)
            .issueDate(issueDate)
            .expiryDate(expiryDate)
            .path(subdir + "/" + filename)
            .build();
        return documentRepo.save(doc);
    }

    /**
     * Recupera il logo aziendale come Document
     */
    @Transactional(readOnly = true)
    public Optional<Document> getCompanyLogo(Long companyId) {
        return documentRepo.findByTypeAndPathContaining(DocumentType.COMPANY_LOGO, "company/" + companyId + "/")
                .stream()
                .findFirst();
    }

    /**
     * Recupera tutti i documenti aziendali
     */
    @Transactional(readOnly = true)
    public List<Document> getCompanyDocuments(Long companyId) {
        return documentRepo.findByTypeAndPathContaining(DocumentType.COMPANY_LOGO, "company/" + companyId + "/");
    }

    /**
     * Carica la risorsa per un documento aziendale
     */
    @Transactional(readOnly = true)
    public Resource loadCompanyDocumentAsResource(Long companyId, String filename) {
        String dir = "company/" + companyId + "/docs";
        return fileStorage.loadAsResource(filename, dir);
    }
}
