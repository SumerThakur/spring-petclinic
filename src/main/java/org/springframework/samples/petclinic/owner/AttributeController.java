package org.springframework.samples.petclinic.owner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/pets/{petId}/attributes")
public class AttributeController {

	private final AttributeRepository attributeRepository;

	private final OwnerRepository ownerRepository;

	@Autowired
	public AttributeController(AttributeRepository attributeRepository, OwnerRepository ownerRepository) {
		this.attributeRepository = attributeRepository;
		this.ownerRepository = ownerRepository;
	}

	// List all attributes for a pet
	@GetMapping
	public List<Attribute> getAttributes(@PathVariable Integer petId) {
		return attributeRepository.findByPet_Id(petId);
	}

	// Get a single attribute by id for a pet
	@GetMapping("/{attributeId}")
	public ResponseEntity<Attribute> getAttribute(@PathVariable Integer petId, @PathVariable Integer attributeId) {
		Optional<Attribute> attr = attributeRepository.findById(attributeId);
		return attr.filter(attribute -> attribute.getPet() != null && attribute.getPet().getId().equals(petId))
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.notFound().build());
	}

	// Create a new attribute for a pet
	@PostMapping
	public ResponseEntity<Attribute> createAttribute(@PathVariable Integer petId, @RequestBody Attribute attribute) {
		attribute.setId(null); // Ensure new
		Pet pet = findPetById(petId);
		attribute.setPet(pet);
		Attribute saved = attributeRepository.save(attribute);
		return ResponseEntity.ok(saved);
	}

	// Update an attribute for a pet
	@PutMapping("/{attributeId}")
	public ResponseEntity<Attribute> updateAttribute(@PathVariable Integer petId, @PathVariable Integer attributeId,
			@RequestBody Attribute updated) {
		Optional<Attribute> attrOpt = attributeRepository.findById(attributeId);
		if (attrOpt.isEmpty())
			return ResponseEntity.notFound().build();
		Attribute attr = attrOpt.get();
		if (attr.getPet() == null || !attr.getPet().getId().equals(petId))
			return ResponseEntity.badRequest().build();
		attr.setName(updated.getName());
		attr.setAttrValue(updated.getAttrValue());
		Attribute saved = attributeRepository.save(attr);
		return ResponseEntity.ok(saved);
	}

	// Delete an attribute for a pet
	@DeleteMapping("/{attributeId}")
	public ResponseEntity<Void> deleteAttribute(@PathVariable Integer petId, @PathVariable Integer attributeId) {
		Optional<Attribute> attrOpt = attributeRepository.findById(attributeId);
		if (attrOpt.isEmpty())
			return ResponseEntity.notFound().build();
		Attribute attr = attrOpt.get();
		if (attr.getPet() == null || !attr.getPet().getId().equals(petId))
			return ResponseEntity.badRequest().build();
		attributeRepository.delete(attr);
		return ResponseEntity.noContent().build();
	}

	// Batch create attributes for a pet
	@PostMapping("/batch")
	public ResponseEntity<List<Attribute>> createAttributes(@PathVariable Integer petId,
			@RequestBody List<Attribute> attributes) {
		Pet pet = findPetById(petId);
		attributes.forEach(attr -> {
			attr.setId(null);
			attr.setPet(pet);
		});
		List<Attribute> saved = attributeRepository.saveAll(attributes);
		return ResponseEntity.ok(saved);
	}

	// Batch update attributes for a pet
	@PutMapping("/batch")
	public ResponseEntity<List<Attribute>> updateAttributes(@PathVariable Integer petId,
			@RequestBody List<Attribute> updates) {
		Pet pet = findPetById(petId);
		List<Attribute> updatedList = new java.util.ArrayList<>();
		for (Attribute update : updates) {
			if (update.getId() == null)
				continue;
			Optional<Attribute> attrOpt = attributeRepository.findById(update.getId());
			if (attrOpt.isPresent()) {
				Attribute attr = attrOpt.get();
				if (attr.getPet() != null && attr.getPet().getId().equals(petId)) {
					attr.setName(update.getName());
					attr.setAttrValue(update.getAttrValue());
					updatedList.add(attributeRepository.save(attr));
				}
			}
		}
		return ResponseEntity.ok(updatedList);
	}

	// Batch delete attributes for a pet
	@DeleteMapping("/batch")
	public ResponseEntity<Void> deleteAttributes(@PathVariable Integer petId, @RequestBody List<Integer> attributeIds) {
		List<Attribute> toDelete = attributeRepository.findAllById(attributeIds);
		for (Attribute attr : toDelete) {
			if (attr.getPet() != null && attr.getPet().getId().equals(petId)) {
				attributeRepository.delete(attr);
			}
		}
		return ResponseEntity.noContent().build();
	}

	// Helper to find a pet by id
	private Pet findPetById(Integer petId) {
		// Find the owner that has this pet
		return ownerRepository.findAll()
			.stream()
			.flatMap(owner -> owner.getPets().stream())
			.filter(pet -> pet.getId().equals(petId))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Pet not found with id: " + petId));
	}

}