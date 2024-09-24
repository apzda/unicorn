package com.apzda.cloud.uc.domain.repository;

import com.apzda.cloud.uc.domain.entity.RoleChild;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface RoleChildRepository extends CrudRepository<RoleChild, Long> {

    long deleteByRoleId(Long roleId);

    List<RoleChild> findByChildId(Long childId);

    boolean existsByRoleIdAndChildId(Long roleId, Long childId);

    long deleteByChildId(Long childId);

}
