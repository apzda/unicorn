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
public abstract class ErrorCode {

    public static final int ACCOUNT_NOT_FOUND = 902000;

    public static final int CONFIG_SYNC_ERROR = 902001;

    public static final int PWD_IS_INVALID = 902002;

    public static final int PWD_NOT_MATCH = 902003;

    public static final int PWD_IS_BLANK = 902004;

    public static final int PWD_SAME_AS_ORIGINAL = 902005;

    public static final int PHONE_IS_OCCUPIED = 902006;

    public static final int EMAIL_IS_OCCUPIED = 902007;

    public static final int USERNAME_IS_OCCUPIED = 902008;

    public static final int ILLEGAL_CHILD = 902010;

    public static final int HAS_CHILDREN = 902011;

    public static final int ROLE_IS_OCCUPIED = 902012;

    public static final int BUILTIN_ROLE = 902013;

    public static final int BUILTIN_PRIVILEGE = 902014;

    public static final int ALREADY_SWITCHED = 902015;

    public static final int NOT_SWITCHED = 902016;

    public static final int GRANT_CODE_NOT_MATCH = 902017;

    public static final int AUTHENTICATOR_NOT_SET = 902018;

    public static final int MFA_NOT_INITIALIZED = 902019;

    public static final int MFA_NOT_MATCH = 902020;

    public static final int REC_CODE_MISSING = 902021;

    public static final int REC_CODE_NOT_FOUND = 902022;

    public static final int REC_CODE_MAX_EXCEEDED = 902023;

    public static final int PHONE_MISSING = 902024;

    public static final int SMS_CODE_ERROR = 902025;

    public static final int EMAIL_IS_MISSING = 902026;

}
