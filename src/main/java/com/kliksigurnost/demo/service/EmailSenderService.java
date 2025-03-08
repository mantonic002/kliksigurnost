package com.kliksigurnost.demo.service;

public interface EmailSenderService {
    void sendEmail(String to, String subject, String body);
}
