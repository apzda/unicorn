/*
 * Copyright (C) 2023-2024 Fengz Ning (windywany@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.apzda.cloud.uc.service;

import com.apzda.cloud.audit.aop.AuditLog;
import com.apzda.cloud.gsvc.context.TenantManager;
import com.apzda.cloud.gsvc.error.NotBlankError;
import com.apzda.cloud.gsvc.error.NotFoundError;
import com.apzda.cloud.gsvc.error.ServiceError;
import com.apzda.cloud.gsvc.exception.GsvcException;
import com.apzda.cloud.gsvc.ext.GsvcExt;
import com.apzda.cloud.uc.domain.entity.Privilege;
import com.apzda.cloud.uc.domain.entity.RoleChild;
import com.apzda.cloud.uc.domain.entity.RolePrivilege;
import com.apzda.cloud.uc.domain.mapper.PrivilegeMapper;
import com.apzda.cloud.uc.domain.mapper.RoleMapper;
import com.apzda.cloud.uc.domain.repository.PrivilegeRepository;
import com.apzda.cloud.uc.domain.repository.RoleChildRepository;
import com.apzda.cloud.uc.domain.repository.RolePrivilegeRepository;
import com.apzda.cloud.uc.domain.repository.RoleRepository;
import com.apzda.cloud.uc.error.DeleteBuiltinRoleError;
import com.apzda.cloud.uc.error.HasChildError;
import com.apzda.cloud.uc.error.IllegalRoleChildError;
import com.apzda.cloud.uc.error.RoleOccupiedError;
import com.apzda.cloud.uc.proto.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Service
@RequiredArgsConstructor
@Slf4j
public class RoleServiceImpl implements RoleService {

    private final RoleChildRepository roleChildRepository;

    private final RoleRepository roleRepository;

    private final PrivilegeRepository privilegeRepository;

    private final RolePrivilegeRepository rolePrivilegeRepository;

    private final RoleMapper roleMapper;

    private final PrivilegeMapper privilegeMapper;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public RoleQueryResponse list(RoleQuery query) {
        val builder = RoleQueryResponse.newBuilder().setErrCode(0);
        val current = Math.max(1, query.getCurrent()) - 1;
        val size = Math.max(10, query.getSize());
        val pageRequest = PageRequest.of(current, size);
        pageRequest.withSort(Sort.Direction.ASC, "role");

        ExampleMatcher matcher = ExampleMatcher.matchingAll()
            .withMatcher("tenantId", ExampleMatcher.GenericPropertyMatchers.exact());
        val probe = new com.apzda.cloud.uc.domain.entity.Role();
        probe.setTenantId(TenantManager.tenantId(0L));

        if (StringUtils.isNotBlank(query.getRole())) {
            matcher = matcher.withMatcher("role", ExampleMatcher.GenericPropertyMatchers.contains());
            probe.setRole(query.getRole());
        }
        if (StringUtils.isNotBlank(query.getName())) {
            matcher = matcher.withMatcher("name", ExampleMatcher.GenericPropertyMatchers.contains());
            probe.setName(query.getName());
        }

        val example = Example.of(probe, matcher);
        val roles = roleRepository.findAll(example, pageRequest);
        val records = roles.getContent().stream().map(role -> {
            val ab = Role.newBuilder();
            ab.setId(String.valueOf(role.getId()));
            ab.setRole(role.getRole());
            ab.setName(role.getName());
            ab.setBuiltin(Boolean.TRUE.equals(role.getBuiltin()));
            ab.setTenantId(String.valueOf(role.getTenantId()));
            if (role.getDescription() != null) {
                ab.setDescription(role.getDescription());
            }
            val children = role.getChildren();
            if (!CollectionUtils.isEmpty(children)) {
                ab.addAllChildren(role.getChildren().stream().map(roleMapper::fromEntity).toList());
            }
            val parents = role.allParents();
            if (!CollectionUtils.isEmpty(parents)) {
                ab.addAllParents(parents.stream().map(roleMapper::fromEntity).toList());
            }
            val privileges = role.getPrivileges();
            if (!CollectionUtils.isEmpty(privileges)) {
                ab.addAllGranted(privileges.stream().map(privilegeMapper::fromEntity).toList());
            }
            return ab.build();
        }).toList();

        builder.addAllResults(records);
        builder.setTotalPage(roles.getTotalPages());
        return builder.setCurrent(query.getCurrent()).setTotalRecord(roles.getTotalElements()).setSize(size).build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Modifying(clearAutomatically = true)
    @AuditLog(activity = "创建角色", template = "角色'{}({})'创建成功", errorTpl = "角色'{}({})'创建失败: {}",
            args = { "#request.name", "#request.role", "#throwExp?.message" })
    public GsvcExt.CommonRes create(RoleDto request) {
        val role = new com.apzda.cloud.uc.domain.entity.Role();
        role.setTenantId(TenantManager.tenantId(0L));
        role.setName(request.getName());
        role.setRole(request.getRole());
        role.setBuiltin(false);
        role.setProvider("db");
        role.setDescription(request.getDescription());

        val builder = GsvcExt.CommonRes.newBuilder();
        // 保存角色
        try {
            roleRepository.save(role);
            // 保存权限
            updateRolePrivileges(request.getGrantedList(), role);
            // 保存下级角色
            updateRoleChildren(request.getChildrenList(), role);
            // 刷新到库
            entityManager.flush();
        }
        catch (GsvcException ge) {
            throw ge;
        }
        catch (Exception e) {
            val message = e.getMessage();
            if (message != null && message.contains("UDX_ROLE")) {
                throw new GsvcException(new RoleOccupiedError(request.getRole()), e);
            }
            throw new GsvcException(ServiceError.SERVICE_ERROR, e);
        }
        return builder.setErrCode(0).build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Modifying(clearAutomatically = true)
    @AuditLog(activity = "修改角色", template = "角色'{}({})'修改成功", errorTpl = "角色'{}({})'修改失败: {}",
            args = { "#request.name", "#request.role", "#throwExp?.message" })
    public GsvcExt.CommonRes update(RoleDto request) {
        val id = request.getId();
        if (StringUtils.isBlank(id)) {
            val notBlankError = new NotBlankError("id");
            throw new GsvcException(notBlankError);
        }
        val opt = roleRepository.findById(Long.valueOf(id));
        if (opt.isEmpty()) {
            val notFoundError = new NotFoundError("{resource.role}", id);
            throw new GsvcException(notFoundError);
        }
        val role = opt.get();
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        // 保存角色
        try {
            roleRepository.save(role);
            // 保存权限
            updateRolePrivileges(request.getGrantedList(), role);
            // 保存下级角色
            updateRoleChildren(request.getChildrenList(), role);
            // 刷新到库
            entityManager.flush();
        }
        catch (GsvcException ge) {
            throw ge;
        }
        catch (Exception e) {
            throw new GsvcException(ServiceError.SERVICE_ERROR, e);
        }
        val builder = GsvcExt.CommonRes.newBuilder();
        return builder.setErrCode(0).build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @AuditLog(activity = "删除角色", template = "角色'{}'已经删除", errorTpl = "角色'{}'删除失败: {}",
            args = { "#request.id", "#throwExp?.message" })
    public GsvcExt.CommonRes delete(RoleId request) {
        val id = request.getId();
        if (StringUtils.isBlank(id)) {
            val notBlankError = new NotBlankError("id");
            throw new GsvcException(notBlankError);
        }
        val opt = roleRepository.findById(Long.valueOf(id));
        if (opt.isEmpty()) {
            val notFoundError = new NotFoundError("{resource.role}", id);
            throw new GsvcException(notFoundError);
        }
        val builder = GsvcExt.CommonRes.newBuilder();
        val role = opt.get();
        // 不能删除内置角色
        if (Boolean.TRUE.equals(role.getBuiltin())) {
            throw new GsvcException(new DeleteBuiltinRoleError(role.getName()));
        }
        // 不能删除有子级角色的角色
        if (!CollectionUtils.isEmpty(role.getChildren())) {
            throw new GsvcException(new HasChildError(role.getName()));
        }
        // 小心
        val roleId = role.getId();
        // 删除角色
        roleRepository.delete(role);
        // 删除角色的授权
        rolePrivilegeRepository.deleteByRoleId(roleId);
        // 删除角色分配
        roleChildRepository.deleteByChildId(roleId);

        return builder.setErrCode(0).build();
    }

    private void updateRoleChildren(List<String> childrenList, com.apzda.cloud.uc.domain.entity.Role role) {
        val roleId = role.getId();
        if (childrenList.isEmpty()) {
            roleChildRepository.deleteByRoleId(roleId);
            return;
        }
        // 下级角色
        Collection<com.apzda.cloud.uc.domain.entity.Role> children = Optional.ofNullable(role.getChildren())
            .orElse(new ArrayList<>());
        // 上级角色
        val parents = role.allParents();
        // 新的下级角色
        val newChildren = new ArrayList<RoleChild>();
        for (String rid : childrenList) {
            val childId = Long.parseLong(rid);
            val childOpt = roleRepository.findById(childId);
            if (childOpt.isPresent()) {
                // 下级角色存在
                val child = childOpt.get();
                // 判断：下级角色不能是自己且不能是自己的上级角色
                if (child.equals(role) || parents.contains(child)) {
                    val error = new IllegalRoleChildError(role.getName());
                    throw new GsvcException(error);
                }
                if (!roleChildRepository.existsByRoleIdAndChildId(roleId, child.getId())) {
                    // 新的下级角色
                    val rc = new RoleChild();
                    rc.setTenantId(role.getTenantId());
                    rc.setRoleId(roleId);
                    rc.setChildId(child.getId());
                    newChildren.add(rc);
                }
                if (!children.isEmpty()) {
                    // 从原有的下级角色列表中移除已经存在的角色
                    children = children.stream().filter((r) -> !r.getId().equals(childId)).toList();
                }
            }
        }

        if (!children.isEmpty()) {
            // 删除不再包含的下级角色
            for (val oldChild : children) {
                val deleted = roleChildRepository.deleteByChildId(oldChild.getId());
                log.trace("assigned child({}) deleted: {}", oldChild, deleted > 0);
            }
        }

        if (!newChildren.isEmpty()) {
            roleChildRepository.saveAll(newChildren);
        }
    }

    private void updateRolePrivileges(List<String> grantedList, com.apzda.cloud.uc.domain.entity.Role role) {
        Collection<Privilege> granted = Optional.ofNullable(role.getPrivileges()).orElse(new ArrayList<>());
        val privileges = new ArrayList<RolePrivilege>();
        for (val rid : grantedList) {
            val pid = Long.parseLong(rid);
            Optional<Privilege> privilege = privilegeRepository.findById(pid);
            if (privilege.isPresent()) {
                // 权限存在
                val entity = privilege.get();
                if (!rolePrivilegeRepository.existsByRoleIdAndPrivilegeId(role.getId(), entity.getId())) {
                    // 未授权
                    val rolePrivilege = new RolePrivilege();
                    rolePrivilege.setTenantId(entity.getTenantId());
                    rolePrivilege.setRoleId(role.getId());
                    rolePrivilege.setPrivilegeId(entity.getId());
                    privileges.add(rolePrivilege);
                }
                if (!granted.isEmpty()) {
                    // 从原有的授权列表中移除已经存在的授权
                    granted = granted.stream().filter((r) -> !r.getId().equals(pid)).toList();
                }
            }
        }

        if (!granted.isEmpty()) {
            // 删除不再分配的权限
            for (Privilege privilege : granted) {
                val deleted = rolePrivilegeRepository.deleteByRoleIdAndPrivilegeId(role.getId(), privilege.getId());
                log.trace("granted permission({}) of Role({}) deleted: {}", privilege.getPermission(), role.getRole(),
                        deleted > 0);
            }
        }

        if (!privileges.isEmpty()) {
            rolePrivilegeRepository.saveAll(privileges);
        }
    }

}
