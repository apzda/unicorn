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
import com.apzda.cloud.gsvc.exception.GsvcException;
import com.apzda.cloud.gsvc.ext.GsvcExt;
import com.apzda.cloud.uc.domain.entity.Privilege;
import com.apzda.cloud.uc.domain.entity.SecurityResource;
import com.apzda.cloud.uc.domain.mapper.PrivilegeMapper;
import com.apzda.cloud.uc.domain.repository.PrivilegeRepository;
import com.apzda.cloud.uc.domain.repository.RolePrivilegeRepository;
import com.apzda.cloud.uc.domain.repository.SecurityResourceRepository;
import com.apzda.cloud.uc.error.DeleteBuiltinPrivilegeError;
import com.apzda.cloud.uc.proto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;

import static org.springframework.data.domain.ExampleMatcher.GenericPropertyMatchers.contains;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Service
@RequiredArgsConstructor
@Slf4j
public class PrivilegeServiceImpl implements PrivilegeService {

    private final PrivilegeRepository privilegeRepository;

    private final RolePrivilegeRepository rolePrivilegeRepository;

    private final SecurityResourceRepository securityResourceRepository;

    private final PrivilegeMapper privilegeMapper;

    @Override
    @PreAuthorize("@authz.iCan('r:privilege')")
    @Transactional(readOnly = true)
    public PrivilegeQueryRes query(PrivilegeQuery query) {
        val builder = PrivilegeQueryRes.newBuilder().setErrCode(0);
        val current = Math.max(1, query.getCurrent()) - 1;
        val size = Math.max(10, query.getSize());
        val pageRequest = PageRequest.of(current, size);
        pageRequest.withSort(Sort.Direction.ASC, "name");

        ExampleMatcher matcher = null;
        val probe = new com.apzda.cloud.uc.domain.entity.Privilege();
        val search = query.getQuery();
        if (StringUtils.isNotBlank(search)) {
            matcher = ExampleMatcher.matchingAny()
                .withMatcher("name", contains())
                .withMatcher("permission", contains())
                .withMatcher("type", contains())
                .withIgnorePaths("deleted");
            probe.setName(search);
            probe.setPermission(search);
            probe.setType(search);
        }

        Page<Privilege> results;
        if (matcher != null) {
            val example = Example.of(probe, matcher);
            results = privilegeRepository.findAll(example, pageRequest);
        }
        else {
            results = privilegeRepository.findAll(pageRequest);
        }
        val records = results.getContent().stream().map(privilegeMapper::fromEntity).toList();

        builder.addAllResults(records);
        builder.setTotalPage(results.getTotalPages());
        return builder.setCurrent(query.getCurrent()).setTotalRecord(results.getTotalElements()).setSize(size).build();
    }

    @Override
    @PreAuthorize("@authz.iCan('c:privilege')")
    @Transactional
    @Modifying
    @AuditLog(activity = "创建权限", template = "权限'{}({})'创建成功", errorTpl = "权限'{}({})'创建失败: {}",
            args = { "#request.name", "#request.permission", "#throwExp?.message" })
    public GsvcExt.CommonRes create(PrivilegeDto request) {
        val builder = GsvcExt.CommonRes.newBuilder().setErrCode(0);
        val privilege = new Privilege();
        privilege.setName(request.getName());
        privilege.setTenantId(TenantManager.tenantId(0L));
        privilege.setPermission(request.getPermission());
        privilege.setType(request.getType());
        privilege.setDescription(request.getDescription());
        privilege.setBuiltin(false);
        privilege.setExtra(request.getExtra());
        privilegeRepository.save(privilege);
        return builder.build();
    }

    @Override
    @PreAuthorize("@authz.iCan('u:privilege')")
    @Transactional
    @Modifying
    @AuditLog(activity = "修改权限", template = "权限'{}'修改成功", errorTpl = "权限'{}'修改失败: {}",
            args = { "#request.id", "#throwExp?.message" })
    public GsvcExt.CommonRes update(PrivilegeDto request) {
        val builder = GsvcExt.CommonRes.newBuilder();
        val id = request.getId();
        if (StringUtils.isBlank(id)) {
            val notBlankError = new NotBlankError("id");
            throw new GsvcException(notBlankError);
        }
        val privilegeOpt = privilegeRepository.findById(Long.parseLong(id));
        if (privilegeOpt.isEmpty()) {
            val notFoundError = new NotFoundError("{resource.privilege}", id);
            throw new GsvcException(notFoundError);
        }
        val privilege = privilegeOpt.get();
        privilege.setName(request.getName());
        privilege.setPermission(request.getPermission());
        privilege.setDescription(request.getDescription());
        privilege.setExtra(request.getExtra());
        privilegeRepository.save(privilege);
        return builder.setErrCode(0).build();
    }

    @Override
    @PreAuthorize("@authz.iCan('d:privilege')")
    @Transactional
    @Modifying
    @AuditLog(activity = "删除权限", template = "权限'{}'删除成功", errorTpl = "权限'{}'删除失败: {}",
            args = { "#request.id", "#throwExp?.message" })
    public GsvcExt.CommonRes delete(PrivilegeId request) {
        val builder = GsvcExt.CommonRes.newBuilder();
        val id = request.getId();
        if (StringUtils.isBlank(id)) {
            val notBlankError = new NotBlankError("id");
            throw new GsvcException(notBlankError);
        }
        val privilegeOpt = privilegeRepository.findById(Long.parseLong(id));
        if (privilegeOpt.isEmpty()) {
            val notFoundError = new NotFoundError("{resource.privilege}", id);
            throw new GsvcException(notFoundError);
        }
        val privilege = privilegeOpt.get();
        if (Boolean.TRUE.equals(privilege.getBuiltin())) {
            throw new GsvcException(new DeleteBuiltinPrivilegeError(privilege.getName()));
        }
        // 删除权限
        privilegeRepository.delete(privilege);
        // 删除授权
        rolePrivilegeRepository.deleteByPrivilegeId(privilege.getId());

        return builder.setErrCode(0).build();
    }

    @Override
    @Transactional(readOnly = true)
    public ResourceRes resource(ResourceReq req) {
        val builder = ResourceRes.newBuilder().setErrCode(0);
        val pid = req.getPid();
        val resources = securityResourceRepository.findByPidOrderByNameAsc(pid);
        if (!CollectionUtils.isEmpty(resources)) {
            builder.addAllResource(toResourceVos(resources));
        }
        return builder.build();
    }

    private Collection<ResourceVo> toResourceVos(Collection<SecurityResource> resources) {
        val resourceVos = new ArrayList<ResourceVo>();
        if (!CollectionUtils.isEmpty(resources)) {
            for (SecurityResource resource : resources) {
                val rb = ResourceVo.newBuilder();
                rb.setId(resource.getRid());
                rb.setName(resource.getName());
                rb.setActions(StringUtils.defaultIfBlank(resource.getActions(), "c,r,u,d"));
                if (resource.getDescription() != null) {
                    rb.setDescription(resource.getDescription());
                }
                if (resource.getExplorer() != null) {
                    rb.setExplorer(resource.getExplorer());
                }
                rb.addAllChildren(toResourceVos(resource.getChildren()));
                resourceVos.add(rb.buildPartial());
            }
        }
        return resourceVos;
    }

}
