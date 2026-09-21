package com.newtron.newtron_workforce_backend.dto;

public interface NearbyWorkerProjection {
    Long getWorkerId();
    String getFullName();
    String getPhotoStorageKey();
    Boolean getIsAvailableOnDemand();
    Double getDistanceKm();
    String getMainSkill();
    Integer getExperienceYears();
}
