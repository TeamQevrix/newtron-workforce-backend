package com.newtron.newtron_workforce_backend.controller;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.auth.enums.Role;
import com.newtron.newtron_workforce_backend.auth.enums.UserStatus;
import com.newtron.newtron_workforce_backend.auth.repository.UserRepository;
import com.newtron.newtron_workforce_backend.common.exception.ValidationException;
import com.newtron.newtron_workforce_backend.dto.JobDetailsDto;
import com.newtron.newtron_workforce_backend.entity.Job;
import com.newtron.newtron_workforce_backend.entity.JobPhoto;
import com.newtron.newtron_workforce_backend.entity.Team;
import com.newtron.newtron_workforce_backend.entity.WorkerProfile;
import com.newtron.newtron_workforce_backend.repository.JobPhotoRepository;
import com.newtron.newtron_workforce_backend.repository.JobRepository;
import com.newtron.newtron_workforce_backend.repository.TeamRepository;
import com.newtron.newtron_workforce_backend.repository.WorkerProfileRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class JobDetailsIntegrationTest {

    @Autowired
    private JobController jobController;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobPhotoRepository jobPhotoRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkerProfileRepository workerProfileRepository;

    @Autowired
    private TeamRepository teamRepository;

    private User individualWorker;
    private User teamOwnerWorker;
    private User recruiter;
    
    @Autowired
    private com.newtron.newtron_workforce_backend.repository.SkillRepository skillRepository;
    @Autowired
    private com.newtron.newtron_workforce_backend.repository.MasterStateRepository masterStateRepository;
    @Autowired
    private com.newtron.newtron_workforce_backend.repository.MasterDistrictRepository masterDistrictRepository;
    @Autowired
    private com.newtron.newtron_workforce_backend.repository.MasterCityRepository masterCityRepository;

    @BeforeEach
    void setUp() {
        individualWorker = userRepository.save(User.builder().mobile("9999999991").fullName("Ind Worker").role(Role.WORKER).status(UserStatus.ACTIVE).passwordHash("hash").build());
        teamOwnerWorker = userRepository.save(User.builder().mobile("9999999992").fullName("Team Owner").role(Role.WORKER).status(UserStatus.ACTIVE).passwordHash("hash").build());
        recruiter = userRepository.save(User.builder().mobile("9999999993").fullName("Recruiter Name").role(Role.RECRUITER).status(UserStatus.ACTIVE).passwordHash("hash").build());

        WorkerProfile profile1 = workerProfileRepository.save(WorkerProfile.builder()
            .user(individualWorker)
            .fullName("Ind Worker")
            .dateOfBirth(java.time.LocalDate.of(1990, 1, 1))
            .gender(com.newtron.newtron_workforce_backend.enums.Gender.MALE)
            .currentStep(com.newtron.newtron_workforce_backend.enums.OnboardingStep.BASIC_PROFILE)
            .build());
        WorkerProfile profile2 = workerProfileRepository.save(WorkerProfile.builder()
            .user(teamOwnerWorker)
            .fullName("Team Owner")
            .dateOfBirth(java.time.LocalDate.of(1990, 1, 1))
            .gender(com.newtron.newtron_workforce_backend.enums.Gender.MALE)
            .currentStep(com.newtron.newtron_workforce_backend.enums.OnboardingStep.BASIC_PROFILE)
            .build());

        com.newtron.newtron_workforce_backend.entity.Skill skill = skillRepository.save(com.newtron.newtron_workforce_backend.entity.Skill.builder().name("Test Skill").build());
        com.newtron.newtron_workforce_backend.entity.MasterState state = masterStateRepository.save(com.newtron.newtron_workforce_backend.entity.MasterState.builder().name("Test State").build());
        com.newtron.newtron_workforce_backend.entity.MasterDistrict district = masterDistrictRepository.save(com.newtron.newtron_workforce_backend.entity.MasterDistrict.builder().state(state).name("Test District").build());
        com.newtron.newtron_workforce_backend.entity.MasterCity city = masterCityRepository.save(com.newtron.newtron_workforce_backend.entity.MasterCity.builder().district(district).name("Test City").build());

        teamRepository.save(Team.builder()
            .ownerWorkerProfile(profile2)
            .teamName("My Team")
            .primarySkill(skill)
            .state(state)
            .district(district)
            .city(city)
            .workAreaAddress("Test Address")
            .build());
    }

    private UserDetails createAuth(User user) {
        if (user == null) return null;
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getMobile())
                .password(user.getPasswordHash())
                .roles(user.getRole().name())
                .build();
    }

    @Test
    void testZeroPhotos() {
        Job job = jobRepository.save(Job.builder().title("No Photo Job").workMode("INDIVIDUAL").status("Active").build());
        
        JobDetailsDto result = jobController.getJobDetails(job.getId(), null, createAuth(individualWorker));
        
        assertNotNull(result);
        assertNotNull(result.getPhotos());
        assertTrue(result.getPhotos().isEmpty());
    }

    @Test
    void testOnePhoto() {
        Job job = jobRepository.save(Job.builder().title("One Photo Job").workMode("INDIVIDUAL").status("Active").build());
        jobPhotoRepository.save(JobPhoto.builder().job(job).storageKey("jobs/test-photo.jpg").displayOrder(1).build());
        
        JobDetailsDto result = jobController.getJobDetails(job.getId(), null, createAuth(individualWorker));
        
        assertNotNull(result.getPhotos());
        assertEquals(1, result.getPhotos().size());
        assertTrue(result.getPhotos().get(0).contains("/files/jobs/test-photo.jpg"));
        assertFalse(result.getPhotos().get(0).equals("jobs/test-photo.jpg")); // It should be a generated URL
    }

    @Test
    void testMultiplePhotosOrdered() {
        Job job = jobRepository.save(Job.builder().title("Multi Photo Job").workMode("INDIVIDUAL").status("Active").build());
        jobPhotoRepository.save(JobPhoto.builder().job(job).storageKey("jobs/p3.jpg").displayOrder(3).build());
        jobPhotoRepository.save(JobPhoto.builder().job(job).storageKey("jobs/p1.jpg").displayOrder(1).build());
        jobPhotoRepository.save(JobPhoto.builder().job(job).storageKey("jobs/p2.jpg").displayOrder(2).build());
        
        JobDetailsDto result = jobController.getJobDetails(job.getId(), null, createAuth(individualWorker));
        
        assertNotNull(result.getPhotos());
        assertEquals(3, result.getPhotos().size());
        assertTrue(result.getPhotos().get(0).contains("p1.jpg"));
        assertTrue(result.getPhotos().get(1).contains("p2.jpg"));
        assertTrue(result.getPhotos().get(2).contains("p3.jpg"));
    }

    @Test
    void testTeamJobAuthorization() {
        Job teamJob = jobRepository.save(Job.builder().title("Team Job").workMode("TEAM").status("Active").build());
        
        // 1. Team Owner can view
        assertDoesNotThrow(() -> jobController.getJobDetails(teamJob.getId(), null, createAuth(teamOwnerWorker)));
        
        // 2. Individual worker CANNOT view
        ValidationException ex = assertThrows(ValidationException.class, 
            () -> jobController.getJobDetails(teamJob.getId(), null, createAuth(individualWorker)));
        assertEquals("UNAUTHORIZED_ACCESS", ex.getErrorCode());
        
        // 3. Unauthenticated CANNOT view
        ValidationException exUnauth = assertThrows(ValidationException.class, 
            () -> jobController.getJobDetails(teamJob.getId(), null, null));
        assertEquals("UNAUTHORIZED_ACCESS", exUnauth.getErrorCode());
        
        // 4. Recruiter can view (assuming recruiters don't have worker profile constraints)
        assertDoesNotThrow(() -> jobController.getJobDetails(teamJob.getId(), null, createAuth(recruiter)));
    }

    @Test
    void testExistingIndividualJobRegression() {
        Job indJob = jobRepository.save(Job.builder().title("Ind Job").workMode("INDIVIDUAL").status("Active").build());
        
        // Both can view individual jobs
        assertDoesNotThrow(() -> jobController.getJobDetails(indJob.getId(), null, createAuth(individualWorker)));
        assertDoesNotThrow(() -> jobController.getJobDetails(indJob.getId(), null, createAuth(teamOwnerWorker)));
        assertDoesNotThrow(() -> jobController.getJobDetails(indJob.getId(), null, null));
    }
}
