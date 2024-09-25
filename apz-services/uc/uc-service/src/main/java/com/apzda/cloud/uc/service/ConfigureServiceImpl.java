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

import com.apzda.cloud.gsvc.ext.GsvcExt;
import com.apzda.cloud.uc.domain.entity.Privilege;
import com.apzda.cloud.uc.domain.entity.Role;
import com.apzda.cloud.uc.domain.entity.SecurityResource;
import com.apzda.cloud.uc.domain.repository.PrivilegeRepository;
import com.apzda.cloud.uc.domain.repository.RolePrivilegeRepository;
import com.apzda.cloud.uc.domain.repository.RoleRepository;
import com.apzda.cloud.uc.domain.repository.SecurityResourceRepository;
import com.apzda.cloud.uc.properties.SecurityConfigureProperties;
import com.apzda.cloud.uc.proto.ConfigureService;
import com.apzda.cloud.uc.proto.SyncRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Map;

import static com.apzda.cloud.uc.ErrorCode.CONFIG_SYNC_ERROR;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Service
@Slf4j
@RequiredArgsConstructor
public class ConfigureServiceImpl implements ConfigureService {

    private final ObjectMapper objectMapper;

    private final RoleRepository roleRepository;

    private final PrivilegeRepository privilegeRepository;

    private final SecurityResourceRepository securityResourceRepository;

    private final RolePrivilegeRepository rolePrivilegeRepository;

    @Override
    @Transactional
    @Modifying
    public GsvcExt.CommonRes syncConfiguration(SyncRequest request) {
        val configuration = request.getConfiguration();
        log.debug("开始同步安全配置: {}", configuration);
        val builder = GsvcExt.CommonRes.newBuilder();
        builder.setErrCode(0);

        try {
            val properties = objectMapper.readValue(configuration, SecurityConfigureProperties.class);
            syncResources(properties.getResources());
            syncRoles(properties.getRoles());
            syncPrivileges(properties.getPrivileges());
        }
        catch (JsonProcessingException e) {
            builder.setErrCode(CONFIG_SYNC_ERROR);
            builder.setErrMsg(e.getMessage());
            throw new RuntimeException(e);
        }

        return builder.build();
    }

    /**
     * 同步一级资源
     * @param resources 一级资源列表.
     */
    private void syncResources(Map<String, SecurityConfigureProperties.Resource> resources) {
        if (!CollectionUtils.isEmpty(resources)) {
            val parent = new SecurityResource();
            parent.setId(0L);
            for (Map.Entry<String, SecurityConfigureProperties.Resource> resource : resources.entrySet()) {
                val rid = resource.getKey();
                val res = resource.getValue();
                if (Boolean.TRUE.equals(res.getDeleted())) {
                    deleteResource(rid, null);
                }
                else {
                    syncResource(rid, res, parent, null);
                    // 同步一级资源定义的权限
                    val privileges = res.getPrivileges();
                    syncPrivileges(privileges);
                }
            }
        }
    }

    /**
     * 同步资源.
     * @param resources 资源列表
     * @param parent 上级资源
     */
    private void syncResources(Map<String, SecurityConfigureProperties.ResourceNode> resources, SecurityResource parent,
            String pid) {
        if (!CollectionUtils.isEmpty(resources)) {
            for (Map.Entry<String, SecurityConfigureProperties.ResourceNode> resource : resources.entrySet()) {
                val rid = resource.getKey();
                val res = resource.getValue();
                val newPid = pid == null ? rid : pid + "." + rid;
                if (Boolean.TRUE.equals(res.getDeleted())) {
                    deleteResource(rid, newPid);
                }
                else {
                    syncResource(rid, res, parent, newPid);
                }
            }
        }
    }

    /**
     * 同步资源.
     * @param rid 资源ID
     * @param res 资源
     * @param parent 父资源
     */
    private void syncResource(String rid, SecurityConfigureProperties.ResourceNode res, SecurityResource parent,
            String pid) {
        val resource = securityResourceRepository.findByRid(rid);
        val actions = res.getActions();
        val explorer = res.getExplorer();
        SecurityResource securityResource;
        if (resource.isPresent()) {
            securityResource = resource.get();
        }
        else {
            securityResource = new SecurityResource();
            securityResource.setParent(parent);
        }

        securityResource.setRid(rid);
        securityResource.setName(res.getName());
        if (CollectionUtils.isEmpty(actions)) {
            securityResource.setActions("c,r,u,d");
        }
        else {
            securityResource.setActions(String.join(",", actions));
        }

        if (explorer != null) {
            securityResource.setExplorer(explorer.getCanonicalName());
        }
        else {
            securityResource.setExplorer(null);
        }
        securityResource.setDescription(res.getDescription());
        securityResourceRepository.save(securityResource);

        if (!CollectionUtils.isEmpty(res.getChildren())) {
            syncResources(res.getChildren(), securityResource, pid);
        }
    }

    /**
     * 删除资源,同时: 1. 删除子资源 2. 删除授权.
     * @param rid 资源ID
     */
    private void deleteResource(String rid, String pid) {
        val resource = securityResourceRepository.findByRid(rid);
        if (resource.isPresent()) {
            val res = resource.get();
            if (!CollectionUtils.isEmpty(res.getChildren())) {
                for (SecurityResource child : res.getChildren()) {
                    deleteResource(child.getRid(), pid == null ? rid : pid);
                }
            }
            securityResourceRepository.deleteById(res.getId());
            // 删除资源关联的权限
            privilegeRepository.deleteByExtraStartsWith((pid == null ? rid : pid) + ".");
        }
    }

    /**
     * 同步角色列表.
     * @param roles 角色列表.
     */
    private void syncRoles(Map<String, SecurityConfigureProperties.Role> roles) {
        if (!CollectionUtils.isEmpty(roles)) {
            for (Map.Entry<String, SecurityConfigureProperties.Role> entry : roles.entrySet()) {
                val rid = entry.getKey();
                val role = entry.getValue();
                if (Boolean.TRUE.equals(role.getDeleted())) {
                    deleteRole(rid);
                }
                else {
                    syncRole(rid, role);
                }
            }
        }
    }

    /**
     * 同步角色
     * @param rid 角色ID
     * @param role 角色
     */
    private void syncRole(String rid, SecurityConfigureProperties.Role role) {
        val r = roleRepository.findByRoleAndTenantId(rid, 0L);
        val entity = r.orElse(new Role());
        entity.setTenantId(0L);
        entity.setRole(rid);
        entity.setName(role.getName());
        entity.setBuiltin(true);
        entity.setProvider("db");
        entity.setDescription(role.getDescription());
        roleRepository.save(entity);
    }

    /**
     * 删除角色，同时删除授权.
     * @param rid 角色
     */
    private void deleteRole(String rid) {
        val r = roleRepository.findByRoleAndTenantId(rid, 0L);
        if (r.isPresent()) {
            val role = r.get();
            rolePrivilegeRepository.deleteByRoleId(role.getId());
            roleRepository.delete(role);
        }
    }

    /**
     * 同步权限列表.
     * @param privileges 权限列表.
     */
    private void syncPrivileges(Map<String, SecurityConfigureProperties.Privilege> privileges) {
        if (!CollectionUtils.isEmpty(privileges)) {
            for (Map.Entry<String, SecurityConfigureProperties.Privilege> entry : privileges.entrySet()) {
                val permission = entry.getKey();
                val privilege = entry.getValue();
                if (Boolean.TRUE.equals(privilege.getDeleted())) {
                    deletePrivilege(permission);
                }
                else {
                    syncPrivilege(permission, privilege);
                }
            }
        }
    }

    /**
     * 同步权限.
     * @param permission 权限
     * @param privilege 明细
     */
    private void syncPrivilege(String permission, SecurityConfigureProperties.Privilege privilege) {
        val perm = privilegeRepository.findByPermission(permission);
        Privilege priv = perm.orElse(new Privilege());
        priv.setName(privilege.getName());
        val type = privilege.getType();
        if (StringUtils.isNotBlank(type)) {
            priv.setType(type);
        }
        else {
            priv.setType("resource");
        }
        priv.setTenantId(0L);
        priv.setBuiltin(true);
        priv.setPermission(permission);
        priv.setDescription(privilege.getDescription());
        priv.setExtra(privilege.getExtra());
        privilegeRepository.save(priv);
    }

    /**
     * 删除权限,同时删除授权.
     * @param permission 权限
     */
    private void deletePrivilege(String permission) {
        val perm = privilegeRepository.findByPermission(permission);
        if (perm.isPresent()) {
            rolePrivilegeRepository.deleteByPrivilegeId(perm.get().getId());
            privilegeRepository.delete(perm.get());
        }
    }

}
