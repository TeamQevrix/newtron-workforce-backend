package com.newtron.newtron_workforce_backend.repository;

import com.newtron.newtron_workforce_backend.entity.MasterCity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MasterCityRepository extends JpaRepository<MasterCity, Long> {
}
