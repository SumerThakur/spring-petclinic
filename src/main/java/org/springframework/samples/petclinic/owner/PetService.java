package org.springframework.samples.petclinic.owner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PetService {

	private final PetAttributeRepository petAttributeRepository;

	private final PetRepository petRepository;

	@Autowired
	public PetService(PetAttributeRepository petAttributeRepository, PetRepository petRepository) {
		this.petAttributeRepository = petAttributeRepository;
		this.petRepository = petRepository;
	}

	// PetAttribute CRUD
	@Transactional(readOnly = true)
	public List<PetAttribute> getAttributesForPet(Integer petId) {
		return petAttributeRepository.findByPetId(petId);
	}

	@Transactional
	public PetAttribute addAttributeToPet(Integer petId, String name, String value) {
		Pet pet = petRepository.findById(petId).orElseThrow();
		PetAttribute attr = new PetAttribute();
		attr.setPet(pet);
		attr.setName(name);
		attr.setValue(value);
		return petAttributeRepository.save(attr);
	}

	@Transactional
	public PetAttribute updatePetAttribute(Integer attributeId, String name, String value) {
		PetAttribute attr = petAttributeRepository.findById(attributeId).orElseThrow();
		attr.setName(name);
		attr.setValue(value);
		return petAttributeRepository.save(attr);
	}

	@Transactional
	public void deletePetAttribute(Integer attributeId) {
		petAttributeRepository.deleteById(attributeId);
	}

	// Optionally, methods for Pet CRUD can be added here as well
	@Transactional(readOnly = true)
	public Optional<Pet> findPetById(Integer petId) {
		return petRepository.findById(petId);
	}

}