package com.mindrevol.backend.modules.chat.listener;

import com.mindrevol.backend.modules.user.entity.User;
import com.mindrevol.backend.modules.user.service.UserPresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final UserPresenceService userPresenceService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = headerAccessor.getUser();

        if (user instanceof UsernamePasswordAuthenticationToken) {
            User userDetails = (User) ((UsernamePasswordAuthenticationToken) user).getPrincipal();
            if (userDetails != null) {
                userPresenceService.connect(userDetails.getId());
            }
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = headerAccessor.getUser();

        if (user instanceof UsernamePasswordAuthenticationToken) {
            User userDetails = (User) ((UsernamePasswordAuthenticationToken) user).getPrincipal();
            if (userDetails != null) {
                userPresenceService.disconnect(userDetails.getId());
            }
        }
    }
}