package com.apzda.cloud.uc.domain.repository;

import com.apzda.cloud.uc.domain.entity.UserJob;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserJobRepository extends CrudRepository<UserJob, Long> {

}
