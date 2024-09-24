package com.apzda.cloud.uc.domain.repository;

import com.apzda.cloud.uc.domain.entity.User;
import com.apzda.cloud.uc.domain.entity.UserOrganization;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserOrganizationRepository
        extends PagingAndSortingRepository<UserOrganization, Long>, CrudRepository<UserOrganization, Long> {

    List<UserOrganization> findAllByUser(User user);

}
