package com.apzda.cloud.uc.domain.repository;

import com.apzda.cloud.uc.domain.entity.JobLevel;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobLevelRepository extends CrudRepository<JobLevel, Long> {

}
