package com.newtron.newtron_workforce_backend.mapper;

import com.newtron.newtron_workforce_backend.dto.WorkerBasicProfileResponse;
import com.newtron.newtron_workforce_backend.entity.WorkerProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WorkerProfileMapper {

    @Mapping(source = "id", target = "profileId")
    @Mapping(source = "updatedAt", target = "lastUpdatedAt")
    @Mapping(target = "nextStep", ignore = true)
    @Mapping(target = "completionPercentage", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "phone", ignore = true)
    @Mapping(target = "addressSummary", ignore = true)
    @Mapping(target = "mainSkill", ignore = true)
    @Mapping(target = "experienceYears", ignore = true)
    @Mapping(target = "rating", ignore = true)
    @Mapping(target = "jobsCompleted", ignore = true)
    @Mapping(target = "membershipType", ignore = true)
    @Mapping(target = "membershipExpiry", ignore = true)
    @Mapping(target = "verified", ignore = true)
    WorkerBasicProfileResponse toResponse(WorkerProfile profile);
}
