package com.kliksigurnost.demo.service.impl;

import com.kliksigurnost.demo.service.EmailSenderService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class EmailSenderServiceImpl implements EmailSenderService {

    private final JavaMailSender mailSender;
    private final Environment env;

    @Override
    @Async
    public void sendEmail(String to, String subject, String body) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);
            helper.setFrom("aktivacijaracuna@kliksigurnost.com");
            log.info("Sending email to {}", to);
            mailSender.send(mimeMessage);
            log.info("Email sent to {}", to);

        } catch (MessagingException e) {
            log.error("Failed to send email {}", e.getMessage());
            throw new IllegalStateException(env.getProperty("email-send-fail"));
        }
    }
}
