package com.veely.service;

import com.veely.entity.Employment;
import com.veely.entity.EmploymentWorkplace;
import com.veely.entity.Project;
import com.veely.entity.CompanyInfo;
import com.veely.entity.Document;
import com.veely.exception.ResourceNotFoundException;
import com.veely.model.EmploymentStatus;
import com.veely.repository.DocumentRepository;
import com.veely.repository.EmployeeRepository;
import com.veely.repository.EmploymentRepository;
import com.veely.repository.ProjectRepository;
import com.lowagie.text.PageSize;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Chunk;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.DocumentException;
import lombok.extern.slf4j.Slf4j;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.awt.Color;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

/**
 * Servizio per la gestione CRUD di Employment,
 * ricerca/filtro/paginazione e pulizia file/documenti.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EmploymentService {

    private final EmploymentRepository employmentRepo;
    private final DocumentRepository documentRepo;
    private final FileSystemStorageService fileStorage;
    private final EmployeeService employeeService;
    private final EmployeeRepository employeeRepo;
    private final PdfEmploymentService pdfEmploymentService;
    private final ProjectRepository projectRepo;
    
    
    /** Aggiorna lo stato dei rapporti di lavoro scaduti. */
    private void autoTerminateExpired() {
        employmentRepo.markExpiredAsTerminated(LocalDate.now(), EmploymentStatus.TERMINATED);
    }

    /**
     * Crea un nuovo rapporto di lavoro.
     */
    /* ---------- CREATE ---------- */
    public Employment create(Employment employment) {
        // (A)  Aggancia il dipendente se viene passato solo l’id
        if (employment.getEmployee() != null &&
            employment.getEmployee().getId() != null) {
            employment.setEmployee(employeeRepo.findById(employment.getEmployee().getId()).get());
        }
     
        // Associa i luoghi di lavoro e sincronizza i progetti
        if (employment.getWorkplaces() != null) {
            employment.getWorkplaces().forEach(w -> {
                if (w.getProject() != null && w.getProject().getId() != null) {
                    Project proj = projectRepo.findById(w.getProject().getId()).orElse(null);
                    w.setProject(proj);
                }
                w.setEmployment(employment);
            });
        }
     // Associa le iscrizioni sindacali
        if (employment.getUnionMemberships() != null) {
            employment.getUnionMemberships().forEach(u -> u.setEmployment(employment));
        }
        /* (B)  EmploymentAddress è già popolato dal binding – nessun merge manuale necessario */
        if (employment.getEndDate() != null && employment.getEndDate().isBefore(LocalDate.now())) {
            employment.setStatus(EmploymentStatus.TERMINATED);
        }

        /* (B)  EmploymentAddress è già popolato dal binding
                – nessun merge manuale necessario */
        //return employmentRepo.save(employment);
        Employment saved = employmentRepo.save(employment);
        // crea la cartella per i documenti usando la matricola
        fileStorage.initDirectory("employments/" + saved.getMatricola() + "/docs");
        return saved;
    }

    /**
     * Aggiorna un rapporto di lavoro esistente.
     */
    /* ---------- UPDATE ---------- */
    public Employment update(Long id, Employment payload) {

        Employment existing = employmentRepo.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Employment " + id + " not found"));

        /* campi “semplici” */
        existing.setStartDate(payload.getStartDate());
        existing.setEndDate(payload.getEndDate());
        existing.setJobDescription(payload.getJobDescription());
        existing.setSalary(payload.getSalary());
        if (existing.getEndDate() != null && existing.getEndDate().isBefore(LocalDate.now())) {
            existing.setStatus(EmploymentStatus.TERMINATED);
        } else {
            existing.setStatus(payload.getStatus());
        }

        /* campi contrattuali */
        existing.setContractType(payload.getContractType());
        existing.setBranch(payload.getBranch());
        existing.setDepartment(payload.getDepartment());
        existing.setJobTitle(payload.getJobTitle());
        existing.setContractLevel(payload.getContractLevel());
        existing.setCcnl(payload.getCcnl());
        existing.setJobRole(payload.getJobRole());
        
        // Aggiorna i luoghi di lavoro
        existing.getWorkplaces().clear();
        if (payload.getWorkplaces() != null) {
            payload.getWorkplaces().forEach(w -> {
                if (w.getProject() != null && w.getProject().getId() != null) {
                    Project proj = projectRepo.findById(w.getProject().getId()).orElse(null);
                    w.setProject(proj);
                }
                w.setEmployment(existing);
                // Se viene passato un id dall'interfaccia utente, l'entità risultante
                // risulta "detached" e causa un errore al momento del salvataggio.
                // Azzera l'id per creare una nuova associazione gestita.
                w.setId(null);
                existing.getWorkplaces().add(w);
            });
        }
     // Aggiorna le iscrizioni sindacali
        existing.getUnionMemberships().clear();
        if (payload.getUnionMemberships() != null) {
            payload.getUnionMemberships().forEach(u -> {
                u.setEmployment(existing);
                existing.getUnionMemberships().add(u);
            });
        }
        return existing;
    }

    /**
     * Trova un Employment per ID o lancia eccezione se non esiste.
     */
    public Employment findByIdOrThrow(Long id) {
    	 autoTerminateExpired();
         Employment emp = employmentRepo.findByIdWithWorkplaces(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rapporto di lavoro non trovato: " + id));
      // inizializza le iscrizioni sindacali per evitare LazyInitializationException
         if (emp.getUnionMemberships() != null) {
             emp.getUnionMemberships().size();
         }
         return emp;
    }

    /**
     * Ricerca con keyword, stato e commessa (opzionale), paginata.
     */
    public Page<Employment> search(String keyword, EmploymentStatus status, Long projectId, Pageable pageable) {
        autoTerminateExpired();
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return employmentRepo.searchByFilters(kw, status, projectId, LocalDate.now(), pageable);
    }

    public List<Employment> search(String keyword, EmploymentStatus status, Long projectId) {
        return search(keyword, status, projectId, Pageable.unpaged()).getContent();
    }

    /**
     * Esporta l'elenco dei rapporti di lavoro in formato PDF.
     *
     * @return array di byte contenente il documento PDF
     */
    public byte[] exportPdf() {
    	autoTerminateExpired();
        List<Employment> employments = employmentRepo.findAll();

        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        com.lowagie.text.Document pdfDoc = new com.lowagie.text.Document(PageSize.A4);
        try {
            PdfWriter.getInstance(pdfDoc, out);
            pdfDoc.open();

            pdfDoc.add(new Paragraph("Elenco rapporti di lavoro"));
            pdfDoc.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100f);
            table.addCell("ID");
            table.addCell("Matricola");
            table.addCell("Dipendente");
            table.addCell("Job title");
            table.addCell("Data inizio");
            table.addCell("Data fine");
            table.addCell("Stato");

            for (Employment e : employments) {
                table.addCell(String.valueOf(e.getId()));
                table.addCell(e.getMatricola() == null ? "" : e.getMatricola());
                if (e.getEmployee() != null) {
                    table.addCell(e.getEmployee().getFirstName() + " " + e.getEmployee().getLastName());
                } else {
                    table.addCell("");
                }
                table.addCell(e.getJobTitle() == null ? "" : e.getJobTitle());
                table.addCell(e.getStartDate() != null ? e.getStartDate().toString() : "");
                table.addCell(e.getEndDate() != null ? e.getEndDate().toString() : "");
                table.addCell(e.getStatus() != null ? e.getStatus().name() : "");
            }

            pdfDoc.add(table);
            pdfDoc.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Errore nella creazione del PDF", e);
        }

        return out.toByteArray();
    }
    
    /**
     * Elenca tutti i rapporti di lavoro.
     */
    public List<Employment> findAll() {
    	autoTerminateExpired();
        return employmentRepo.findAll();
    }

    /**
     * Elenca tutti i rapporti di lavoro con paginazione.
     */
    public Page<Employment> findAll(Pageable pageable) {
    	autoTerminateExpired();
        return employmentRepo.findAll(pageable);
    }

    /**
     * Filtra per stato del rapporto di lavoro (ACTIVE, TERMINATED, etc) con paginazione.
     */
    @Transactional(readOnly = true)
    public Page<Employment> findByStatus(EmploymentStatus status, Pageable pageable) {
        return employmentRepo.findByStatus(status, pageable);
    }

    /** Conta i rapporti di lavoro per stato. */
    public long countByStatus(EmploymentStatus status) {
        autoTerminateExpired();
        return employmentRepo.countByStatus(status);
    }

    
    /**
     * Ricerca per titolo di lavoro contenente keyword, paginata.
     */
    public Page<Employment> searchByJobTitle(String keyword, Pageable pageable) {
    	autoTerminateExpired();
        String like = "%" + keyword.trim().toLowerCase() + "%";
        return employmentRepo.findByJobTitleIgnoreCaseContaining(like, pageable);
    }

    /**
     * Elimina un rapporto di lavoro e i suoi documenti (DB + filesystem).
     */
    public void delete(Long id) {
        Employment e = findByIdOrThrow(id);
        // Elimina documenti contrattuali associati
        List<Document> docs = documentRepo.findByEmploymentId(id);
        docs.forEach(doc -> {
            String fullPath = doc.getPath();
            int sep = fullPath.lastIndexOf('/');
            String subDir = sep > 0 ? fullPath.substring(0, sep) : "";
            String filename = sep > 0 ? fullPath.substring(sep + 1) : fullPath;
            fileStorage.delete(filename, subDir);
        });
        documentRepo.deleteAll(docs);
        // Rimuove directory fisica
        //fileStorage.deleteDirectory("employments/" + id + "/docs");
        fileStorage.deleteDirectory("employments/" + e.getMatricola() + "/docs");
        // Cancella il rapporto di lavoro
        employmentRepo.delete(e);
    }
    
    public List<Employment> findByEmployeeId(Long employeeId) {
    	autoTerminateExpired();
        return employmentRepo.findByEmployeeId(employeeId);
    }
    
    /**
     * Recupera tutte le Employment per i dipendenti indicati e le raggruppa per employeeId.
     */
    public List<Employment> findByEmployeeIds(List<Long> ids) {
    	autoTerminateExpired();
        if (ids.isEmpty()) return List.of();
        return employmentRepo.findByEmployeeIdIn(ids);
    }
    
    /** Aggiorna la data di fine rapporto di lavoro. */
    public void updateEndDate(Long id, LocalDate date) {
        Employment e = findByIdOrThrow(id);
        e.setEndDate(date);
    }
    
    /**
     * Esporta l'elenco dei rapporti di lavoro in formato PDF con design SINGOL.
     * Delega la generazione PDF al service dedicato.
     * 
     * @return array di byte contenente il documento PDF
     */
    public byte[] exportStyledPdf(String keyword, EmploymentStatus status, Long projectId) throws IOException {
        List<Employment> employments = search(keyword, status, projectId);
        return pdfEmploymentService.exportStyledPdf(employments);
    }
}
