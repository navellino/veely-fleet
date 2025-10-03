package com.veely.repository;

import com.veely.entity.ProjectContact;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectContactRepository extends JpaRepository<ProjectContact, Long> {
}
