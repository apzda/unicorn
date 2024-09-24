package com.apzda.cloud.uc.domain.repository;

import com.apzda.cloud.uc.domain.entity.Department;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentRepository extends CrudRepository<Department, Long> {

}
