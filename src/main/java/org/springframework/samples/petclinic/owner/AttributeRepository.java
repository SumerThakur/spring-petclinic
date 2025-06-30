package org.springframework.samples.petclinic.owner;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AttributeRepository extends JpaRepository<Attribute, Integer> {

	List<Attribute> findByPet_Id(Integer petId);

}
