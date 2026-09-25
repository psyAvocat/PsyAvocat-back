package com.psyavocat.service;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FirebaseMessagingServiceTest {

    @Mock
    private FirebaseMessaging firebaseMessaging;

    @Mock
    private ObjectProvider<FirebaseMessaging> firebaseMessagingProvider;

    private FirebaseMessagingService messagingService;

    @BeforeEach
    void setUp() {
        when(firebaseMessagingProvider.getIfAvailable()).thenReturn(firebaseMessaging);
        messagingService = new FirebaseMessagingService(firebaseMessagingProvider);
    }

    @Test
    @DisplayName("sendToDevice - Succès de l'envoi vers un appareil unique")
    void sendToDevice_Success() throws Exception {
        when(firebaseMessaging.send(any(Message.class))).thenReturn("projects/psyavocat/messages/msg-12345");

        String result = messagingService.sendToDevice(
                "fcm-device-token-abc",
                "Rendez-vous confirmé",
                "Votre rendez-vous avec Me Dupont a été validé.",
                Map.of("dossierId", "d-101", "type", "APPOINTMENT_CONFIRMED")
        );

        assertNotNull(result);
        assertEquals("projects/psyavocat/messages/msg-12345", result);
        verify(firebaseMessaging, times(1)).send(any(Message.class));
    }

    @Test
    @DisplayName("sendToDevice - Gestion de l'erreur lorsque FirebaseMessaging est absent")
    void sendToDevice_ProviderReturnsNull() {
        when(firebaseMessagingProvider.getIfAvailable()).thenReturn(null);

        String result = messagingService.sendToDevice("token-test", "Titre", "Corps", null);
        assertNull(result);
    }

    @Test
    @DisplayName("sendToMultipleDevices - Succès de l'envoi multicast")
    void sendToMultipleDevices_Success() throws Exception {
        BatchResponse mockBatchResponse = mock(BatchResponse.class);
        when(mockBatchResponse.getSuccessCount()).thenReturn(2);
        when(mockBatchResponse.getFailureCount()).thenReturn(0);
        when(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(mockBatchResponse);

        BatchResponse response = messagingService.sendToMultipleDevices(
                List.of("token-1", "token-2"),
                "Alerte PsyAvocat",
                "Nouveau document partagé",
                Map.of("documentId", "doc-55")
        );

        assertNotNull(response);
        assertEquals(2, response.getSuccessCount());
        assertEquals(0, response.getFailureCount());
        verify(firebaseMessaging, times(1)).sendEachForMulticast(any(MulticastMessage.class));
    }

    @Test
    @DisplayName("sendDataMessage - Succès de l'envoi de données silencieuses")
    void sendDataMessage_Success() throws Exception {
        when(firebaseMessaging.send(any(Message.class))).thenReturn("data-msg-999");

        String result = messagingService.sendDataMessage(
                "fcm-device-token-xyz",
                Map.of("action", "REFRESH_DOSSIER", "dossierId", "dos-99")
        );

        assertEquals("data-msg-999", result);
    }
}
