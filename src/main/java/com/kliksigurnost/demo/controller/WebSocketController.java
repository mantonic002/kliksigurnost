package com.kliksigurnost.demo.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {

    @MessageMapping("/sendNotification")
    @SendTo("/queue/notifications")
    public String sendNotification(String message) {
        return message;
    }
}