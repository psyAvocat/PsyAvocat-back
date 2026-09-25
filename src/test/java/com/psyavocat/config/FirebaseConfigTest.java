package com.psyavocat.config;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FirebaseConfigTest {

    @Autowired(required = false)
    private FirebaseApp firebaseApp;

    @Autowired(required = false)
    private FirebaseAuth firebaseAuth;

    @Autowired(required = false)
    private FirebaseMessaging firebaseMessaging;

    @Test
    @DisplayName("Vérifie que FirebaseApp, FirebaseAuth et FirebaseMessaging sont injectés en singleton")
    void testFirebaseBeansInitialization() {
        assertNotNull(firebaseApp, "FirebaseApp doit être instancié");
        assertNotNull(firebaseAuth, "FirebaseAuth doit être instancié");
        assertNotNull(firebaseMessaging, "FirebaseMessaging doit être instancié");
        assertEquals("psyavocat-test", firebaseApp.getOptions().getProjectId());
    }
}
