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
package com.apzda.cloud.uc;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public interface UserMetas {

    String CURRENT_TENANT_ID = "CUR_TENANT_ID";

    String CURRENT_ORG_ID = "CUR_ORG_ID";

    String CURRENT_DEPT_ID = "CUR_DEPT_ID";

    String CURRENT_JOB_ID = "CUR_JOB_ID";

    String CURRENT_THEME_ID = "THEME";

    String TIMEZONE_KEY = "TIMEZONE";

    String RUNNING_AS = "running_as";

    String RUNNING_GT = "running_gt";

    String LANGUAGE_KEY = "language";

}
