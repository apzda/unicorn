package com.apzda.cloud.uc.domain.repository;

import com.apzda.cloud.uc.domain.entity.UserDepartment;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserDepartmentRepository extends CrudRepository<UserDepartment, Long> {

}
