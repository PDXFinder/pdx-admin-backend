package org.pdxfinder.repositories;

import org.pdxfinder.Treatment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransTreatmentRepository extends JpaRepository<Treatment, Integer> {

}
