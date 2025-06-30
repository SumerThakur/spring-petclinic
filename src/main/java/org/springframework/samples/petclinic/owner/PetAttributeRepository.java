package org.springframework.samples.petclinic.owner;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PetAttributeRepository extends JpaRepository<PetAttribute, Integer> {

	List<PetAttribute> findByPetId(Integer petId);

}