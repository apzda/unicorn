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
package com.apzda.cloud.captcha.provider;

import com.apzda.cloud.captcha.SerializableStream;
import com.google.common.cache.CacheLoader;
import jakarta.annotation.Nonnull;
import lombok.val;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
class ImageCacheLoader extends CacheLoader<Integer, SerializableStream> {

    private final List<File> images;

    public ImageCacheLoader(List<File> images) {
        this.images = images;
    }

    @Override
    @Nonnull
    public SerializableStream load(@Nonnull Integer key) throws Exception {
        val file = images.get(key);
        try (val input = new FileInputStream(file)) {
            return new SerializableStream(input);
        }
    }

}
