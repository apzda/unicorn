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
package cn.apzda.cloud.audit.controller;

import cn.apzda.cloud.audit.TestVo;
import com.apzda.cloud.audit.aop.AuditContextHolder;
import com.apzda.cloud.audit.aop.AuditLog;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@RestController
@RequiredArgsConstructor
public class DemoController {

    @AuditLog(activity = "test", message = "#{'you are get then id is: ' + #id +', then result is:' + #returnObj }")
    @GetMapping("/audit/{id}")
    public String shouldBeAudited(@PathVariable String id) {
        return shouldBeAudited1(id);
    }

    @AuditLog(activity = "test", message = "#{'new = '+ #newValue}")
    public String shouldBeAudited1(String id) {
        val context = AuditContextHolder.getContext();
        context.setNewValue("1");
        return shouldBeAudited2(id);
    }

    @AuditLog(activity = "test", message = "#{'old = ' + #oldValue}")
    public String shouldBeAudited2(String id) {
        val context = AuditContextHolder.getContext();
        context.setOldValue("2");
        return "hello ya:" + id;
    }

    @AuditLog(activity = "test", message = "#{'new = '+ #newValue}")
    public String shouldBeAudited3(String id) {
        val context = AuditContextHolder.getContext();
        val tv = new TestVo();
        tv.setPhone("13088888888");
        context.setNewValue(tv);
        return shouldBeAudited2(id);
    }

}
