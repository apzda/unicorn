/*
 * Copyright (C) 2023-2023 Fengz Ning (windywany@gmail.com)
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
package com.apzda.cloud.uc.context;

import com.apzda.cloud.gsvc.context.TenantManager;
import com.apzda.cloud.gsvc.security.userdetails.UserDetailsMeta;
import com.google.common.base.Splitter;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@RequiredArgsConstructor
public class UCenterTenantManager extends TenantManager<String> {

    @Override
    @NonNull
    protected String[] getTenantIds() {
        val authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            val principal = authentication.getPrincipal();
            if (principal instanceof UserDetailsMeta userDetailsMeta) {
                val currentTenantId = userDetailsMeta.getTenantId();
                if (StringUtils.isNotBlank(currentTenantId)) {
                    return Splitter.on(",")
                        .trimResults()
                        .omitEmptyStrings()
                        .splitToList(currentTenantId)
                        .toArray(new String[] {});
                }
            }
        }
        return new String[] { null };
    }

}
