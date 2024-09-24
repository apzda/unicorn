package com.apzda.cloud.uc.domain.repository;

import com.apzda.cloud.uc.domain.entity.Job;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRepository extends CrudRepository<Job, Long> {

}
