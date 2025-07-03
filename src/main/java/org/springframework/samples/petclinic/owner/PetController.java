/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.owner;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.HashMap;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.validation.Valid;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpServletRequest;

/**
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Wick Dynex
 */
@Controller
@RequestMapping("/owners/{ownerId}")
class PetController {

	private static final String VIEWS_PETS_CREATE_OR_UPDATE_FORM = "pets/createOrUpdatePetForm";

	private final OwnerRepository owners;

	private final PetTypeRepository types;

	public PetController(OwnerRepository owners, PetTypeRepository types) {
		this.owners = owners;
		this.types = types;
	}

	@ModelAttribute("types")
	public Collection<PetType> populatePetTypes() {
		return this.types.findPetTypes();
	}

	@ModelAttribute("owner")
	public Owner findOwner(@PathVariable("ownerId") int ownerId) {
		Optional<Owner> optionalOwner = this.owners.findByIdWithPetsAndAttributes(ownerId);
		Owner owner = optionalOwner.orElseThrow(() -> new IllegalArgumentException(
				"Owner not found with id: " + ownerId + ". Please ensure the ID is correct "));
		return owner;
	}

	@ModelAttribute("pet")
	public Pet findPet(@ModelAttribute("owner") Owner owner,
			@PathVariable(name = "petId", required = false) Integer petId, HttpServletRequest request) {
		// If this is a POST request, return a new Pet instance for binding
		if ("POST".equalsIgnoreCase(request.getMethod())) {
			return new Pet();
		}
		if (petId == null) {
			return new Pet();
		}
		Pet pet = owner.getPet(petId);
		if (pet != null && pet.getAttributes() != null) {
			pet.getAttributes().size(); // Force initialization
		}
		return pet;
	}

	@InitBinder("owner")
	public void initOwnerBinder(WebDataBinder dataBinder) {
		dataBinder.setDisallowedFields("id");
	}

	@InitBinder("pet")
	public void initPetBinder(WebDataBinder dataBinder) {
		dataBinder.setValidator(new PetValidator());
	}

	@GetMapping("/pets/new")
	public String initCreationForm(Owner owner, ModelMap model) {
		Pet pet = new Pet();
		owner.addPet(pet);
		// Sort attributes by order (nulls last) if any
		if (pet.getAttributes() != null) {
			pet.getAttributes()
				.sort(java.util.Comparator.comparing(a -> a.getOrder() == null ? Integer.MAX_VALUE : a.getOrder()));
		}
		return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
	}

	@PostMapping("/pets/new")
	public String processCreationForm(Owner owner, @Valid Pet pet, BindingResult result,
			RedirectAttributes redirectAttributes) {

		// Log attributes if present
		if (pet.getClass().getDeclaredFields() != null) {
			try {
				var field = pet.getClass().getDeclaredField("attributes");
				field.setAccessible(true);
				Object attributes = field.get(pet);
				System.out.println("Pet attributes (creation): " + attributes);
			}
			catch (NoSuchFieldException | IllegalAccessException e) {
				System.out.println("No attributes field found in Pet or unable to access.");
			}
		}

		if (StringUtils.hasText(pet.getName()) && pet.isNew() && owner.getPet(pet.getName(), true) != null)
			result.rejectValue("name", "duplicate", "already exists");

		LocalDate currentDate = LocalDate.now();
		if (pet.getBirthDate() != null && pet.getBirthDate().isAfter(currentDate)) {
			result.rejectValue("birthDate", "typeMismatch.birthDate");
		}

		if (result.hasErrors()) {
			return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
		}

		// Set pet reference in each attribute and filter out empty attributes
		if (pet.getAttributes() != null) {
			pet.getAttributes()
				.removeIf(attr -> !StringUtils.hasText(attr.getName()) && !StringUtils.hasText(attr.getAttrValue()));
			pet.getAttributes().forEach(attr -> attr.setPet(pet));
		}

		owner.addPet(pet);
		this.owners.save(owner); // Cascade will save attributes as well
		redirectAttributes.addFlashAttribute("message", "New Pet has been Added");
		return "redirect:/owners/{ownerId}";
	}

	@GetMapping("/pets/{petId}/edit")
	public String initUpdateForm(Owner owner, @PathVariable("petId") int petId, ModelMap model) {
		Pet pet = owner.getPet(petId);
		if (pet == null) {
			throw new IllegalArgumentException("Pet not found with id: " + petId);
		}
		// Ensure attributes are loaded and initialized for the form
		if (pet.getAttributes() == null) {
			pet.setAttributes(new ArrayList<>());
		}
		else {
			pet.getAttributes().size(); // Force initialization if LAZY
		}
		// Sort attributes by order (nulls last)
		pet.getAttributes()
			.sort(java.util.Comparator.comparing(a -> a.getOrder() == null ? Integer.MAX_VALUE : a.getOrder()));
		model.put("pet", pet);
		return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
	}

	@PostMapping("/pets/{petId}/edit")
	public String processUpdateForm(Owner owner, @Valid Pet pet, BindingResult result,
			RedirectAttributes redirectAttributes, @PathVariable("petId") int petId) {

		// Log only the owner, pet, and petId
		System.out.println("--- processUpdateForm call ---");
		System.out.println("owner: " + owner);
		System.out.println("pet: " + pet);
		System.out.println("petId: " + petId);
		System.out.println("------------------------------");

		// Log attributes if present
		if (pet.getClass().getDeclaredFields() != null) {
			try {
				var field = pet.getClass().getDeclaredField("attributes");
				field.setAccessible(true);
				Object attributes = field.get(pet);
				System.out.println("Pet attributes (update): " + attributes);
			}
			catch (NoSuchFieldException | IllegalAccessException e) {
				System.out.println("No attributes field found in Pet or unable to access.");
			}
		}

		String petName = pet.getName();

		// checking if the pet name already exists for the owner
		if (StringUtils.hasText(petName)) {
			Pet existingPet = owner.getPet(petName, false);
			if (existingPet != null && !existingPet.getId().equals(pet.getId())) {
				result.rejectValue("name", "duplicate", "already exists");
			}
		}

		LocalDate currentDate = LocalDate.now();
		if (pet.getBirthDate() != null && pet.getBirthDate().isAfter(currentDate)) {
			result.rejectValue("birthDate", "typeMismatch.birthDate");
		}

		if (result.hasErrors()) {
			return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
		}

		// Improved attribute update logic with 'order' field
		Pet existingPet = owner.getPet(petId);
		if (existingPet != null) {
			existingPet.setName(pet.getName());
			existingPet.setBirthDate(pet.getBirthDate());
			existingPet.setType(pet.getType());

			// Map existing attributes by id for quick lookup
			Map<Integer, Attribute> existingAttrMap = new HashMap<>();
			for (Attribute attr : existingPet.getAttributes()) {
				if (attr.getId() != null) {
					existingAttrMap.put(attr.getId(), attr);
				}
			}

			// Track which attributes are present in the form
			List<Attribute> updatedAttributes = new ArrayList<>();
			List<Integer> seenIds = new ArrayList<>();
			if (pet.getAttributes() != null) {
				for (Attribute formAttr : pet.getAttributes()) {
					if (!StringUtils.hasText(formAttr.getName()) && !StringUtils.hasText(formAttr.getAttrValue())) {
						continue; // skip empty
					}
					if (formAttr.getId() != null && existingAttrMap.containsKey(formAttr.getId())) {
						// Update existing attribute
						Attribute existingAttr = existingAttrMap.get(formAttr.getId());
						existingAttr.setName(formAttr.getName());
						existingAttr.setAttrValue(formAttr.getAttrValue());
						existingAttr.setOrder(formAttr.getOrder());
						updatedAttributes.add(existingAttr);
						seenIds.add(formAttr.getId());
					}
					else if (formAttr.getOrder() != null) {
						// New attribute with order
						Attribute newAttr = new Attribute();
						newAttr.setName(formAttr.getName());
						newAttr.setAttrValue(formAttr.getAttrValue());
						newAttr.setOrder(formAttr.getOrder());
						newAttr.setPet(existingPet);
						updatedAttributes.add(newAttr);
					}
				}
			}
			// Remove any attribute not present in the form (by id)
			existingPet.getAttributes().clear();
			updatedAttributes.sort((a, b) -> {
				if (a.getOrder() == null && b.getOrder() == null)
					return 0;
				if (a.getOrder() == null)
					return 1;
				if (b.getOrder() == null)
					return -1;
				return Integer.compare(a.getOrder(), b.getOrder());
			});
			existingPet.getAttributes().addAll(updatedAttributes);
		}
		System.out.println(
				"Before save, existingPet attributes: " + (existingPet != null ? existingPet.getAttributes() : null));
		this.owners.save(owner);
		System.out.println(
				"After save, existingPet attributes: " + (existingPet != null ? existingPet.getAttributes() : null));
		redirectAttributes.addFlashAttribute("message", "Pet details has been edited");
		return "redirect:/owners/{ownerId}";
	}

}
