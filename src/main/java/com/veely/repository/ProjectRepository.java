package com.veely.repository;

import com.veely.entity.Project;
import com.veely.model.ProjectStatus;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    
    /**
     * Trova tutte le commesse con lo stato specificato.
     *
     * @param status stato della commessa da filtrare
     * @return elenco di commesse che corrispondono allo stato
     */
    List<Project> findByStatus(ProjectStatus status);
}
