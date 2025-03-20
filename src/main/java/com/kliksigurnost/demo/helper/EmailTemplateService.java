package com.kliksigurnost.demo.helper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class EmailTemplateService {
    private final TemplateEngine templateEngine;

    public String buildAccVerificationEmail(String name, String link) {
        Context context = new Context();
        context.setVariable("name", name);
        context.setVariable("link", link);
        return templateEngine.process("email-verification", context);
    }

    public String buildPasswordResetEmail(String name, String link) {
        Context context = new Context();
        context.setVariable("name", name);
        context.setVariable("link", link);
        return templateEngine.process("password-reset", context);
    }
}