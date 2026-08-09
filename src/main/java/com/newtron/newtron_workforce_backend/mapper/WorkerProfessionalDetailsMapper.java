package com.newtron.newtron_workforce_backend.mapper;

import com.newtron.newtron_workforce_backend.dto.WorkerProfessionalDetailsResponse;
import com.newtron.newtron_workforce_backend.dto.WorkerSkillDto;
import com.newtron.newtron_workforce_backend.entity.WorkerProfessionalDetails;
import com.newtron.newtron_workforce_backend.entity.WorkerSkill;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WorkerProfessionalDetailsMapper {

    @Mapping(source = "highestQualification.id", target = "highestQualificationId")
    @Mapping(source = "highestQualification.name", target = "highestQualificationName")
    @Mapping(target = "skills", ignore = true)
    @Mapping(target = "nextStep", ignore = true)
    @Mapping(target = "completionPercentage", ignore = true)
    @Mapping(target = "currentStep", ignore = true)
    WorkerProfessionalDetailsResponse toResponse(WorkerProfessionalDetails details);

    @Mapping(source = "skill.id", target = "skillId")
    @Mapping(source = "skill.name", target = "skillName")
    WorkerSkillDto toSkillDto(WorkerSkill workerSkill);
}
