package com.psyavocat.service;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    @Test
    @DisplayName("Notifications : priorité haute et canal Android de l'application (affichage application fermée)")
    void notifications_prioriteHauteEtCanal() throws Exception {
        BatchResponse reponse = mock(BatchResponse.class);
        org.mockito.ArgumentCaptor<MulticastMessage> captor = org.mockito.ArgumentCaptor.forClass(MulticastMessage.class);
        when(firebaseMessaging.sendEachForMulticast(captor.capture())).thenReturn(reponse);

        messagingService.sendToMultipleDevices(List.of("t1"), "Rappel", "Rendez-vous demain", Map.of());

        Object android = champDeType(captor.getValue(), com.google.firebase.messaging.AndroidConfig.class);
        assertNotNull(android, "configuration Android absente");
        assertEquals("high", champ(android, "priority"));
        Object notification = champDeType(android, com.google.firebase.messaging.AndroidNotification.class);
        assertEquals(FirebaseMessagingService.CANAL_ANDROID, champ(notification, "channelId"));
        assertNotNull(champDeType(captor.getValue(), com.google.firebase.messaging.ApnsConfig.class),
                "configuration iOS absente");
    }

    /** Les messages FCM n'exposent pas leurs champs : lecture par réflexion (test uniquement). */
    private static Object champDeType(Object objet, Class<?> type) throws IllegalAccessException {
        for (java.lang.reflect.Field f : objet.getClass().getDeclaredFields()) {
            if (type.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                return f.get(objet);
            }
        }
        return null;
    }

    private static Object champ(Object objet, String nom) throws ReflectiveOperationException {
        java.lang.reflect.Field f = objet.getClass().getDeclaredField(nom);
        f.setAccessible(true);
        return f.get(objet);
    }
}
