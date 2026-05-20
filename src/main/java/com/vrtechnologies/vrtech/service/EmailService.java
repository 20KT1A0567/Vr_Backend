package com.vrtechnologies.vrtech.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.admin.otp.from:}")
    private String defaultFromAddress;

    @Value("${app.admin.otp.from-name:VR Technologies}")
    private String defaultFromName;

    public EmailService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public boolean isConfigured() {
        return mailSender != null && normalizeFromAddress(defaultFromAddress) != null;
    }

    public void sendHtml(String to, String subject, String htmlBody) {
        if (mailSender == null) {
            throw new IllegalStateException("Mail sender is not configured. Set MAIL_USERNAME and MAIL_APP_PASSWORD env vars.");
        }
        String fromAddress = normalizeFromAddress(defaultFromAddress);
        String fromName = normalizeFromName(defaultFromAddress, defaultFromName);
        if (fromAddress == null) {
            throw new IllegalStateException("Mail 'from' address is not configured. Set app.admin.otp.from or MAIL_USERNAME.");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(new InternetAddress(fromAddress, fromName));
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Sent HTML email to {} subject='{}'", to, subject);
        } catch (MessagingException | UnsupportedEncodingException ex) {
            log.error("Failed to send email to {}: {}", to, ex.getMessage(), ex);
            throw new RuntimeException("Failed to send email: " + ex.getMessage(), ex);
        }
    }

    private String normalizeFromAddress(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            InternetAddress[] parsed = InternetAddress.parse(value.trim(), false);
            if (parsed.length == 0 || parsed[0].getAddress() == null || parsed[0].getAddress().isBlank()) {
                return null;
            }
            return parsed[0].getAddress().trim();
        } catch (MessagingException ex) {
            return extractBracketedAddress(value);
        }
    }

    private String normalizeFromName(String addressValue, String configuredName) {
        if (configuredName != null && !configuredName.isBlank()) {
            return configuredName.trim();
        }
        if (addressValue == null || addressValue.isBlank()) {
            return "VR Technologies";
        }
        try {
            InternetAddress[] parsed = InternetAddress.parse(addressValue.trim(), false);
            if (parsed.length > 0 && parsed[0].getPersonal() != null && !parsed[0].getPersonal().isBlank()) {
                return parsed[0].getPersonal().trim();
            }
        } catch (MessagingException ignored) {
        }
        return "VR Technologies";
    }

    private String extractBracketedAddress(String value) {
        String trimmed = value.trim();
        int start = trimmed.lastIndexOf('<');
        int end = trimmed.indexOf('>', start + 1);
        if (start >= 0 && end > start) {
            String address = trimmed.substring(start + 1, end).trim();
            return address.isBlank() ? null : address;
        }
        return trimmed;
    }
}
