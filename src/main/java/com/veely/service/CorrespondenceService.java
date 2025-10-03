package com.veely.service;

import com.veely.entity.Correspondence;
import com.veely.model.CorrespondenceType;
import com.veely.repository.CorrespondenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.veely.exception.ResourceNotFoundException;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CorrespondenceService {
    private final CorrespondenceRepository repo;

    public Correspondence register(CorrespondenceType tipo, int progress,
            String descrizione,
            LocalDate data,
            String sender,
            String recipient,
            String notes) {
        int anno = LocalDate.now().getYear();
        Integer max = repo.findMaxProgressivo(anno, tipo);
        int progressivo;
        if(progress == 0) {
        	progressivo = (max == null) ? 1 : max + 1;
        }else {
        	progressivo = progress;
        }
        

        Correspondence c = Correspondence.builder()
                .anno(anno)
                .progressivo(progressivo)
                .tipo(tipo)
                .descrizione(descrizione)
                .data(data)
                .sender(sender)
                .recipient(recipient)
                .notes(notes)
                .build();
        return repo.save(c);
    }

    public String formatProtocol(Correspondence c) {
        return String.format("%03d/%d", c.getProgressivo(), c.getAnno());
    }

    @Transactional(readOnly = true)
    public List<Correspondence> getAll() {
        return repo.findAll();
    }
    
    @Transactional(readOnly = true)
    public java.util.List<Correspondence> searchByType(int year, CorrespondenceType tipo, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return repo.findByAnnoAndTipoOrderByProgressivoDesc(year, tipo);
        }
        return repo.searchByAnnoAndTipoAndKeyword(year, tipo, keyword.toLowerCase());
    }
    
    @Transactional(readOnly = true)
    public Correspondence findByIdOrThrow(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Protocollo non trovato: " + id));
    }

    @Transactional(readOnly = true)
    public List<Correspondence> search(int year, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return repo.findByAnnoOrderByProgressivoDesc(year);
        }
        return repo.searchByAnnoAndKeyword(year, keyword.toLowerCase());
    }
    
    @Transactional(readOnly = true)
    public List<Integer> getYears() {
        List<Integer> years = new java.util.ArrayList<>(repo.findDistinctAnni());
        int current = LocalDate.now().getYear();
        if (!years.contains(current)) {
            years.add(0, current);
        }
        return years;
    }
    
    public Correspondence update(Long id, int progressivo, CorrespondenceType tipo, String descrizione,
            LocalDate data, String sender,
            String recipient,
            String notes) {
					Correspondence c = findByIdOrThrow(id);
					c.setProgressivo(progressivo);
					c.setTipo(tipo);
					c.setDescrizione(descrizione);
					c.setData(data);
					c.setSender(sender);
					c.setRecipient(recipient);
					c.setNotes(notes);
					return c;
					}
								
			public void delete(Long id) {
				Correspondence c = findByIdOrThrow(id);
				repo.delete(c);
			}
		
			 @Transactional(readOnly = true)
			    public String getLastProtocol(CorrespondenceType type) {
			        int year = LocalDate.now().getYear();
			        return repo.findFirstByAnnoAndTipoOrderByProgressivoDesc(year, type)
			                .map(this::formatProtocol)
			                .orElse("--");
			    }

			    public String getLastIncomingProtocol() {
			        return getLastProtocol(CorrespondenceType.E);
			    }

			    public String getLastOutgoingProtocol() {
			        return getLastProtocol(CorrespondenceType.U);
			    }

    
}