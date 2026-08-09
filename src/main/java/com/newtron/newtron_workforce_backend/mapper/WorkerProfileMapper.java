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
    WorkerBasicProfileResponse toResponse(WorkerProfile profile);
}
