package com.apzda.cloud.uc.domain.repository;

import com.apzda.cloud.uc.domain.entity.Organization;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationRepository extends CrudRepository<Organization, Long> {

}
