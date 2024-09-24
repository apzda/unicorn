package com.apzda.cloud.captcha;

import com.apzda.cloud.gsvc.io.Base64DecodeMultipartFile;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.io.InputStream;

@Data
@NoArgsConstructor
public class SerializableStream {

    private String base64;

    public SerializableStream(InputStream inputStream) throws IOException {
        this.base64 = Base64DecodeMultipartFile.inputStreamToStream(inputStream);
    }

}
