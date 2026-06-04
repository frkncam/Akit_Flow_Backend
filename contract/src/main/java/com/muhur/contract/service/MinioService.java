package com.muhur.contract.service;

import java.io.InputStream;

public interface MinioService {

    void upload(String key, InputStream is, long size, String contentType);

    String presignedGetUrl(String key);

    byte[] download(String key);

    void delete(String key);
}
