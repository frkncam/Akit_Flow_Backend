package com.akitflow.common.query;

/** Geçersiz filtre ifadesi (bilinmeyen alan, hatalı UUID/sayı/tarih/enum) durumunda fırlatılır → HTTP 400. */
public class PredicateException extends RuntimeException {
    public PredicateException(String message) {
        super(message);
    }
}
