package com.veely.service;

import com.veely.dto.payslip.PayslipSendResult;
import com.veely.entity.UniqueCertification;
import com.veely.model.PayslipStatus;
import com.veely.repository.UniqueCertificationRepository;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.eclipse.angus.mail.util.MailConnectException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UniqueCertificationEmailService {

    private final JavaMailSender mailSender;
    private final UniqueCertificationRepository uniqueCertificationRepository;
    private final FileSystemStorageService fileSystemStorageService;

    @Value("risorseumane@sincolsrl.it")
    private String fromAddress;

    public PayslipSendResult sendCertifications(Collection<Long> certificationIds, String subject, String body) {
        PayslipSendResult result = PayslipSendResult.builder().build();
        if (CollectionUtils.isEmpty(certificationIds)) {
            return result;
        }

        result.setRequested(certificationIds.size());
        List<UniqueCertification> certifications = uniqueCertificationRepository.findByIdIn(certificationIds);
        String trimmedSubject = subject != null ? subject.trim() : "";
        String trimmedBody = body != null ? body.trim() : "";

        for (UniqueCertification certification : certifications) {
            if (certification.getEmployee() == null || !StringUtils.hasText(certification.getEmployee().getEmail())) {
                String message = String.format("Certificazione Unica %s non inviata: nessun dipendente/email associato",
                        certification.getFiscalCode());
                result.addSkipped(message);
                certification.setStatus(PayslipStatus.UNMATCHED);
                certification.setLastError(message);
                continue;
            }

            String finalSubject = trimmedSubject;
            String finalBody = trimmedBody;

            try {
                sendEmailWithAttachment(certification, finalSubject, finalBody);
                certification.setStatus(PayslipStatus.SENT);
                certification.setSentAt(java.time.LocalDateTime.now());
                certification.setSentTo(certification.getEmployee().getEmail());
                certification.setLastSubject(finalSubject);
                certification.setLastBody(finalBody);
                certification.setLastError(null);
                result.setSent(result.getSent() + 1);
            } catch (MessagingException | IOException | MailException ex) {
                log.error("Errore durante l'invio della Certificazione Unica {}", certification.getFiscalCode(), ex);
                certification.setStatus(PayslipStatus.FAILED);
                certification.setLastError(ex.getMessage());
                result.addError("Errore inviando la Certificazione Unica " + certification.getFiscalCode() + ": " + ex.getMessage());
            }
        }

        return result;
    }

    private void sendEmailWithAttachment(UniqueCertification certification, String subject, String body)
            throws MessagingException, IOException {
        Path path = Path.of(certification.getStoragePath());
        String filename = path.getFileName().toString();
        String subdir = path.getParent() == null ? "" : path.getParent().toString();
        Resource resource = fileSystemStorageService.loadAsResource(filename, subdir);
        String attachmentName = StringUtils.hasText(certification.getOriginalFilename())
                ? certification.getOriginalFilename()
                : resource.getFilename();

        MimeMessagePreparator preparator = mimeMessage -> {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress);
            helper.setTo(certification.getEmployee().getEmail());
            helper.setSubject(subject);
            helper.setText(body, false);
            helper.addAttachment(attachmentName, resource);
        };

        try {
            mailSender.send(preparator);
        } catch (MailException ex) {
            if (!retryWithImplicitSsl(preparator, ex)) {
                throw ex;
            }
        }
    }

    private boolean retryWithImplicitSsl(MimeMessagePreparator preparator, MailException ex) {
        if (!(mailSender instanceof JavaMailSenderImpl primarySender)) {
            return false;
        }

        if (!shouldRetryWithImplicitSsl(primarySender, ex)) {
            return false;
        }

        JavaMailSenderImpl fallbackSender = buildImplicitSslSender(primarySender);
        if (fallbackSender == null) {
            return false;
        }

        try {
            fallbackSender.send(preparator);
            log.info("Invio Certificazione Unica riuscito usando fallback SSL su porta 465 per host {}", fallbackSender.getHost());
            applyFallbackConfiguration(primarySender, fallbackSender);
            return true;
        } catch (MailException retryException) {
            ex.addSuppressed(retryException);
            log.error("Tentativo di fallback SSL fallito verso {}", fallbackSender.getHost(), retryException);
            return false;
        }
    }

    private boolean shouldRetryWithImplicitSsl(JavaMailSenderImpl primarySender, Throwable throwable) {
        if (primarySender.getPort() == 465) {
            return false;
        }
        return isConnectionRefused(throwable) || isSocketTimeout(throwable);
    }

    private boolean isConnectionRefused(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof MailConnectException) {
                Throwable nested = current.getCause();
                if (nested instanceof ConnectException connectException) {
                    String message = connectException.getMessage();
                    return message != null && message.toLowerCase(Locale.ROOT).contains("connection refused");
                }
                return true;
            }
            if (current instanceof ConnectException connectException) {
                String message = connectException.getMessage();
                if (message != null && message.toLowerCase(Locale.ROOT).contains("connection refused")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isSocketTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private JavaMailSenderImpl buildImplicitSslSender(JavaMailSenderImpl primarySender) {
        if (!StringUtils.hasText(primarySender.getHost())) {
            return null;
        }

        JavaMailSenderImpl fallback = new JavaMailSenderImpl();
        fallback.setHost(primarySender.getHost());
        fallback.setUsername(primarySender.getUsername());
        fallback.setPassword(primarySender.getPassword());
        fallback.setProtocol(primarySender.getProtocol());
        fallback.setDefaultEncoding(primarySender.getDefaultEncoding());
        fallback.setPort(465);

        Properties properties = new Properties();
        Properties sourceProperties = primarySender.getJavaMailProperties();
        if (sourceProperties != null) {
            properties.putAll(sourceProperties);
        }
        properties.put("mail.smtp.socketFactory.port", "465");
        properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        properties.put("mail.smtp.socketFactory.fallback", "false");
        properties.put("mail.smtp.ssl.enable", "true");
        properties.put("mail.smtp.starttls.enable", "false");

        fallback.setJavaMailProperties(properties);
        return fallback;
    }

    private void applyFallbackConfiguration(JavaMailSenderImpl primarySender, JavaMailSenderImpl fallbackSender) {
        primarySender.setPort(fallbackSender.getPort());
        Properties primaryProperties = primarySender.getJavaMailProperties();
        Properties fallbackProperties = fallbackSender.getJavaMailProperties();
        if (primaryProperties != null && fallbackProperties != null) {
            primaryProperties.putAll(fallbackProperties);
        }
    }
}
