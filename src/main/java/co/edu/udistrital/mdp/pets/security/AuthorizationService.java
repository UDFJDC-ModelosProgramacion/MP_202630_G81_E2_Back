package co.edu.udistrital.mdp.pets.security;

import java.util.Objects;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import co.edu.udistrital.mdp.pets.entities.AdoptionEntity;
import co.edu.udistrital.mdp.pets.entities.AdopterEntity;
import co.edu.udistrital.mdp.pets.entities.FollowUpEntity;
import co.edu.udistrital.mdp.pets.entities.MedicalEventEntity;
import co.edu.udistrital.mdp.pets.entities.PetEntity;
import co.edu.udistrital.mdp.pets.entities.ReviewEntity;
import co.edu.udistrital.mdp.pets.entities.ShelterEntity;
import co.edu.udistrital.mdp.pets.entities.UserEntity;
import co.edu.udistrital.mdp.pets.entities.UserRole;
import co.edu.udistrital.mdp.pets.entities.VaccinationRecordEntity;
import co.edu.udistrital.mdp.pets.entities.VaccineEntity;
import co.edu.udistrital.mdp.pets.dto.PetDTO;
import co.edu.udistrital.mdp.pets.dto.ReviewDTO;
import co.edu.udistrital.mdp.pets.dto.VaccinationRecordDTO;
import co.edu.udistrital.mdp.pets.dto.MedicalEventDTO;
import co.edu.udistrital.mdp.pets.dto.FollowUpRequestDTO;
import co.edu.udistrital.mdp.pets.repositories.AdoptionRepository;
import co.edu.udistrital.mdp.pets.repositories.AdopterRepository;
import co.edu.udistrital.mdp.pets.repositories.FollowUpRepository;
import co.edu.udistrital.mdp.pets.repositories.MedicalEventRepository;
import co.edu.udistrital.mdp.pets.repositories.PetRepository;
import co.edu.udistrital.mdp.pets.repositories.ReviewRepository;
import co.edu.udistrital.mdp.pets.repositories.UserRepository;
import co.edu.udistrital.mdp.pets.repositories.VaccinationRecordRepository;
import co.edu.udistrital.mdp.pets.repositories.VaccineRepository;
import co.edu.udistrital.mdp.pets.repositories.VeterinarianRepository;
import lombok.RequiredArgsConstructor;

@Service("authorizationService")
@RequiredArgsConstructor
public class AuthorizationService {

	private final UserRepository userRepository;
	private final AdopterRepository adopterRepository;
	private final PetRepository petRepository;
	private final AdoptionRepository adoptionRepository;
	private final FollowUpRepository followUpRepository;
	private final ReviewRepository reviewRepository;
	private final MedicalEventRepository medicalEventRepository;
	private final VaccinationRecordRepository vaccinationRecordRepository;
	private final VaccineRepository vaccineRepository;
	private final VeterinarianRepository veterinarianRepository;

	public boolean canManageShelter(Authentication authentication, Long shelterId) {
		if (isAdmin(authentication))
			return true;
		UserEntity user = currentUser(authentication);
		return hasRole(authentication, UserRole.SHELTER_ADMIN) && user != null
				&& sameShelter(user.getShelter(), shelterId);
	}

	public boolean canManagePet(Authentication authentication, Long petId) {
		if (isAdmin(authentication))
			return true;
		return petRepository.findById(petId)
				.map(pet -> pet.getShelter() != null
						&& canManageShelter(authentication, pet.getShelter().getId()))
				.orElse(false);
	}

	public boolean canUpdatePet(Authentication authentication, Long petId, PetDTO update) {
		if (!canManagePet(authentication, petId))
			return false;
		if (isAdmin(authentication) || update == null || update.getShelter() == null
				|| update.getShelter().getId() == null)
			return true;
		return petRepository.findById(petId)
				.map(pet -> pet.getShelter() != null
						&& Objects.equals(pet.getShelter().getId(), update.getShelter().getId()))
				.orElse(false);
	}

	public boolean canCreatePet(Authentication authentication, PetDTO pet) {
		return isAdmin(authentication) || pet != null && pet.getShelter() != null
				&& canManageShelter(authentication, pet.getShelter().getId());
	}

	public boolean canAccessAdopter(Authentication authentication, Long adopterId) {
		if (isAdmin(authentication) || owns(authentication, adopterId))
			return true;
		return adopterRepository.findById(adopterId)
				.map(adopter -> adopter.getShelter() != null
						&& canManageShelter(authentication, adopter.getShelter().getId()))
				.orElse(false);
	}

	public boolean canAccessUser(Authentication authentication, Long userId) {
		return isAdmin(authentication) || owns(authentication, userId);
	}

	public boolean canAccessClinicalPet(Authentication authentication, Long petId) {
		if (isAdmin(authentication))
			return true;
		return petRepository.findById(petId)
				.map(pet -> pet.getShelter() != null && (
						canManageShelter(authentication, pet.getShelter().getId())
								|| isVeterinarianForShelter(authentication, pet.getShelter().getId())))
				.orElse(false);
	}

	public boolean canAccessMedicalEvent(Authentication authentication, Long eventId) {
		if (isAdmin(authentication))
			return true;
		return medicalEventRepository.findById(eventId)
				.map(event -> event.getPet() != null
						&& canAccessClinicalPet(authentication, event.getPet().getId()))
				.orElse(false);
	}

	public boolean canCreateMedicalEvent(Authentication authentication, MedicalEventDTO event) {
		return event != null && event.getPet() != null
				&& canUpdateClinicalPet(authentication, event.getPet().getId(), event);
	}

	public boolean canUpdateMedicalEvent(Authentication authentication, Long eventId, MedicalEventDTO update) {
		if (isAdmin(authentication))
			return true;
		return medicalEventRepository.findById(eventId)
				.map(event -> event.getPet() != null
						&& canUpdateClinicalPet(authentication, event.getPet().getId(), update))
				.orElse(false);
	}

	public boolean canUpdateMedicalEventForPet(
			Authentication authentication, Long petId, Long eventId, MedicalEventDTO update) {
		if (isAdmin(authentication))
			return true;
		return medicalEventRepository.findById(eventId)
				.filter(event -> event.getPet() != null && Objects.equals(event.getPet().getId(), petId))
				.map(event -> canUpdateClinicalPet(authentication, petId, update))
				.orElse(false);
	}

	public boolean canCreateMedicalEventForPet(
			Authentication authentication, Long petId, MedicalEventDTO event) {
		return canUpdateClinicalPet(authentication, petId, event);
	}

	public boolean canAccessVaccinationRecord(Authentication authentication, Long recordId) {
		if (isAdmin(authentication))
			return true;
		return vaccinationRecordRepository.findById(recordId)
				.map(record -> record.getPet() != null
						&& canAccessClinicalPet(authentication, record.getPet().getId()))
				.orElse(false);
	}

	public boolean canAccessVaccinationRecordForPet(Authentication authentication, Long petId) {
		return canAccessClinicalPet(authentication, petId);
	}

	public boolean canCreateVaccinationRecord(Authentication authentication, VaccinationRecordDTO record) {
		return record != null && record.getPet() != null
				&& canAccessClinicalPet(authentication, record.getPet().getId());
	}

	public boolean canQueryVaccines(Authentication authentication, Long recordId, Long petId) {
		if (recordId == null && petId == null)
			return true;
		if (isAdmin(authentication))
			return true;
		return (recordId == null || canAccessVaccinationRecord(authentication, recordId))
				&& (petId == null || canAccessClinicalPet(authentication, petId));
	}

	public boolean canAccessVaccine(Authentication authentication, Long vaccineId) {
		if (isAdmin(authentication))
			return true;
		return vaccineRepository.findById(vaccineId)
				.map(vaccine -> vaccine.getVaccinationRecord() != null
						&& canAccessVaccinationRecord(authentication,
								vaccine.getVaccinationRecord().getId()))
				.orElse(false);
	}

	public boolean canAccessFollowUps(Authentication authentication, Long adoptionId) {
		return adoptionRepository.findById(adoptionId)
				.map(adoption -> canAccessAdoption(authentication, adoption))
				.orElse(false);
	}

	public boolean canAccessFollowUp(Authentication authentication, Long adoptionId, Long followUpId) {
		if (isAdmin(authentication))
			return true;
		return followUpRepository.findById(followUpId)
				.filter(followUp -> belongsToAdoption(followUp, adoptionId))
				.map(followUp -> canAccessAdoption(authentication, followUp.getAdoption()))
				.orElse(false);
	}

	public boolean canManageFollowUp(Authentication authentication, Long adoptionId, Long followUpId) {
		if (isAdmin(authentication))
			return true;
		return followUpRepository.findById(followUpId)
				.filter(followUp -> belongsToAdoption(followUp, adoptionId))
				.map(followUp -> canManageAdoption(authentication, followUp.getAdoption()))
				.orElse(false);
	}

	public boolean canCreateFollowUp(
			Authentication authentication, Long adoptionId, FollowUpRequestDTO request) {
		return canManageAdoption(authentication, adoptionId)
				&& canAssignVeterinarian(authentication, adoptionId, request);
	}

	public boolean canUpdateFollowUp(
			Authentication authentication, Long adoptionId, Long followUpId, FollowUpRequestDTO request) {
		return canManageFollowUp(authentication, adoptionId, followUpId)
				&& canAssignVeterinarian(authentication, adoptionId, request);
	}

	public boolean canManageAdoption(Authentication authentication, Long adoptionId) {
		if (isAdmin(authentication))
			return true;
		return adoptionRepository.findById(adoptionId)
				.map(adoption -> canManageAdoption(authentication, adoption))
				.orElse(false);
	}

	public boolean canCreateReview(Authentication authentication, Long petId, Long adoptionId) {
		if (isAdmin(authentication))
			return true;
		Long userId = principalId(authentication);
		if (!hasRole(authentication, UserRole.ADOPTER) || userId == null || adoptionId == null)
			return false;
		return adoptionRepository.findById(adoptionId)
				.map(adoption -> adoption.getAdopter() != null && adoption.getPet() != null
						&& Objects.equals(adoption.getAdopter().getId(), userId)
						&& Objects.equals(adoption.getPet().getId(), petId))
				.orElse(false);
	}

	public boolean canCreateReview(Authentication authentication, Long petId, ReviewDTO review) {
		Long adoptionId = review == null || review.getAdoption() == null
				? null
				: review.getAdoption().getId();
		return canCreateReview(authentication, petId, adoptionId);
	}

	public boolean canManageReview(
			Authentication authentication, Long petId, Long reviewId, Long requestedAdopterId) {
		if (isAdmin(authentication))
			return true;
		Long userId = principalId(authentication);
		if (!hasRole(authentication, UserRole.ADOPTER) || userId == null
				|| !Objects.equals(userId, requestedAdopterId))
			return false;
		return reviewRepository.findById(reviewId)
				.map(review -> belongsToPet(review, petId)
						&& review.getAdoption() != null && review.getAdoption().getAdopter() != null
						&& Objects.equals(review.getAdoption().getAdopter().getId(), userId))
				.orElse(false);
	}

	private boolean canAccessAdoption(Authentication authentication, AdoptionEntity adoption) {
		if (isAdmin(authentication))
			return true;
		Long userId = principalId(authentication);
		if (hasRole(authentication, UserRole.ADOPTER) && userId != null
				&& adoption.getAdopter() != null && Objects.equals(adoption.getAdopter().getId(), userId))
			return true;
		return adoption.getShelter() != null
				&& (canManageShelter(authentication, adoption.getShelter().getId())
						|| isVeterinarianForShelter(authentication, adoption.getShelter().getId()));
	}

	private boolean canManageAdoption(Authentication authentication, AdoptionEntity adoption) {
		return adoption.getShelter() != null
				&& (isAdmin(authentication) || canManageShelter(authentication, adoption.getShelter().getId())
						|| isVeterinarianForShelter(authentication, adoption.getShelter().getId()));
	}

	private boolean canUpdateClinicalPet(
			Authentication authentication, Long petId, MedicalEventDTO event) {
		if (isAdmin(authentication))
			return true;
		if (!canAccessClinicalPet(authentication, petId) || event == null)
			return false;
		Long veterinarianId = event.getVeterinarian() == null ? null : event.getVeterinarian().getId();
		if (veterinarianId == null)
			return true;
		return petRepository.findById(petId)
				.filter(pet -> pet.getShelter() != null)
				.flatMap(pet -> veterinarianRepository.findById(veterinarianId)
						.map(veterinarian -> veterinarian.getShelter() != null
								&& Objects.equals(veterinarian.getShelter().getId(), pet.getShelter().getId())))
				.orElse(false);
	}

	private boolean canAssignVeterinarian(
			Authentication authentication, Long adoptionId, FollowUpRequestDTO request) {
		Long veterinarianId = requestedVeterinarianId(request);
		if (request != null && request.getVeterinarianId() != null && request.getVeterinarian() != null
				&& request.getVeterinarian().getId() != null
				&& !Objects.equals(request.getVeterinarianId(), request.getVeterinarian().getId()))
			return false;
		if (veterinarianId == null || isAdmin(authentication))
			return true;
		return adoptionRepository.findById(adoptionId)
				.filter(adoption -> adoption.getShelter() != null)
				.flatMap(adoption -> veterinarianRepository.findById(veterinarianId)
						.map(veterinarian -> veterinarian.getShelter() != null
								&& Objects.equals(veterinarian.getShelter().getId(), adoption.getShelter().getId())))
				.orElse(false);
	}

	private Long requestedVeterinarianId(FollowUpRequestDTO request) {
		if (request == null)
			return null;
		return request.getVeterinarianId() != null
				? request.getVeterinarianId()
				: request.getVeterinarian() == null ? null : request.getVeterinarian().getId();
	}

	private boolean isVeterinarianForShelter(Authentication authentication, Long shelterId) {
		if (!hasRole(authentication, UserRole.VETERINARIAN))
			return false;
		UserEntity user = currentUser(authentication);
		return user != null && sameShelter(user.getShelter(), shelterId);
	}

	private boolean belongsToAdoption(FollowUpEntity followUp, Long adoptionId) {
		return followUp.getAdoption() != null
				&& Objects.equals(followUp.getAdoption().getId(), adoptionId);
	}

	private boolean belongsToPet(ReviewEntity review, Long petId) {
		return review.getPet() != null && Objects.equals(review.getPet().getId(), petId);
	}

	private boolean owns(Authentication authentication, Long resourceId) {
		return resourceId != null && Objects.equals(principalId(authentication), resourceId);
	}

	private boolean isAdmin(Authentication authentication) {
		return hasRole(authentication, UserRole.ADMIN);
	}

	private boolean hasRole(Authentication authentication, UserRole role) {
		return authentication != null && authentication.getAuthorities().stream()
				.anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role.name()));
	}

	private Long principalId(Authentication authentication) {
		return authentication != null && authentication.getPrincipal() instanceof AppUserPrincipal principal
				? principal.id()
				: null;
	}

	private UserEntity currentUser(Authentication authentication) {
		Long userId = principalId(authentication);
		return userId == null ? null : userRepository.findById(userId).orElse(null);
	}

	private boolean sameShelter(ShelterEntity shelter, Long shelterId) {
		return shelter != null && Objects.equals(shelter.getId(), shelterId);
	}
}
