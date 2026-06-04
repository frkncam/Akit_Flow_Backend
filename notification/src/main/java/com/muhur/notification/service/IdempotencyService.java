package com.muhur.notification.service;

import java.util.UUID;

public interface IdempotencyService {

    /**
     * Idempotency işareti at. Aynı eventId daha önce işlendiyse false döner.
     * Yeni ise true döner ve kayıt eklenir.
     */
    boolean markIfNew(UUID eventId, String eventType);
}
