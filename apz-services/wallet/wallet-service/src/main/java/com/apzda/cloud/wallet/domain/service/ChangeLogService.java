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
package com.apzda.cloud.wallet.domain.service;

import com.apzda.cloud.wallet.domain.entity.ChangeLog;
import com.apzda.cloud.wallet.domain.mapper.ChangeLogMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.val;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Service
public class ChangeLogService extends ServiceImpl<ChangeLogMapper, ChangeLog> {

    @Override
    public boolean removeById(Serializable id) {
        return false;
    }

    @Override
    public boolean removeByIds(Collection<?> list) {
        return false;
    }

    @Override
    public boolean removeById(Serializable id, boolean useFill) {
        return false;
    }

    @Override
    public boolean removeBatchByIds(Collection<?> list, int batchSize) {
        return false;
    }

    @Override
    public boolean removeBatchByIds(Collection<?> list, int batchSize, boolean useFill) {
        return false;
    }

    @Override
    public boolean removeById(ChangeLog entity) {
        return false;
    }

    @Override
    public boolean removeByMap(Map<String, Object> columnMap) {
        return false;
    }

    @Override
    public boolean remove(Wrapper<ChangeLog> queryWrapper) {
        return false;
    }

    @Override
    public boolean removeByIds(Collection<?> list, boolean useFill) {
        return false;
    }

    @Override
    public boolean removeBatchByIds(Collection<?> list) {
        return false;
    }

    @Override
    public boolean removeBatchByIds(Collection<?> list, boolean useFill) {
        return false;
    }

    public ChangeLog getLastLog(Long uid, String currency) {
        val con = Wrappers.lambdaQuery(ChangeLog.class);
        con.eq(ChangeLog::getUid, uid);
        con.eq(ChangeLog::getCurrency, currency);
        con.orderByDesc(ChangeLog::getId);
        con.last("LIMIT 1");

        return getOne(con, false);
    }

}
