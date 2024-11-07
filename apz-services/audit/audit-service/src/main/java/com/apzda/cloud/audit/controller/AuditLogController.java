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
package com.apzda.cloud.audit.controller;

import com.apzda.cloud.audit.proto.AuditService;
import com.apzda.cloud.audit.proto.Query;
import com.apzda.cloud.audit.proto.QueryRes;
import com.apzda.cloud.gsvc.dto.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@RestController
@RequestMapping("/audit-log")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditService auditService;

    @PostMapping("/logs")
    @PreAuthorize("@authz.iCan('r:auditlog')")
    public Response<QueryRes> logs(@RequestBody Query query) {
        return Response.wrap(auditService.logs(query));
    }

    @PostMapping("/my-activities")
    public Response<QueryRes> myActivities(@RequestBody Query query) {
        return Response.wrap(auditService.myLogs(query));
    }

}
