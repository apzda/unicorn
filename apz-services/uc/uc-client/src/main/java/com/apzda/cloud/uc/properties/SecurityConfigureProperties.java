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
package com.apzda.cloud.uc.properties;

import com.apzda.cloud.uc.resource.ResourceIdExplorer;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@ConfigurationProperties(prefix = "apzda.ucenter.security")
@Data
public class SecurityConfigureProperties {

    @JsonIgnore
    private boolean autoSync = true;

    private Map<String, Resource> resources = new HashMap<>();

    private Map<String, Role> roles = new HashMap<>();

    private Map<String, Privilege> privileges = new HashMap<>();

    @EqualsAndHashCode(callSuper = true)
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class Resource extends ResourceNode {

        private Map<String, Privilege> privileges = new HashMap<>();

    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ResourceNode {

        @NotBlank
        private String name;

        private List<String> actions;

        private String description;

        private Boolean deleted;

        private Class<? extends ResourceIdExplorer> explorer;

        private Map<String, ResourceNode> children = new HashMap<>();

    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class Role {

        @NotBlank
        private String name;

        private Boolean deleted;

        private String description;

    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class Privilege {

        @NotBlank
        private String name;

        @NotNull
        private Type type = Type.resource;

        private Boolean deleted = false;

        private String description;

        private String extra;

    }

    public enum Type {

        authority, resource

    }

}
