package com.example.schedulewatcher.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RuzClientTest {

    @Test
    void resolveTeacherIdByTCode_extractsDigitsWithoutHttpCall() {
        RuzClient client = new RuzClient();

        String id = client.resolveTeacherIdByTCode("T-123abc");

        assertEquals("123", id);
    }
}
