package com.veely.service;

import com.veely.dto.payslip.PayslipSendResult;
import com.veely.entity.Payslip;
import com.veely.model.PayslipStatus;
import com.veely.repository.PayslipRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.net.SocketTimeoutException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.eclipse.angus.mail.util.MailConnectException;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PayslipEmailService {

    private final JavaMailSender mailSender;
    private final PayslipRepository payslipRepository;
    private final FileSystemStorageService fileSystemStorageService;
    
    @Value("risorseumane@sincolsrl.it")
    private String fromAddress;

    public PayslipSendResult sendPayslips(Collection<Long> payslipIds, String subject, String body) {
        PayslipSendResult result = PayslipSendResult.builder().build();
        if (CollectionUtils.isEmpty(payslipIds)) {
            return result;
        }

        result.setRequested(payslipIds.size());
        List<Payslip> payslips = payslipRepository.findByIdIn(payslipIds);
        String trimmedSubject = subject != null ? subject.trim() : "";
        String trimmedBody = body != null ? body.trim() : "";

        for (Payslip payslip : payslips) {
            if (payslip.getEmployee() == null || !StringUtils.hasText(payslip.getEmployee().getEmail())) {
                String message = String.format("Cedolino %s non inviato: nessun dipendente/email associato",
                        payslip.getFiscalCode());
                result.addSkipped(message);
                payslip.setStatus(PayslipStatus.UNMATCHED);
                payslip.setLastError(message);
                continue;
            }

            String finalSubject = trimmedSubject;
            String finalBody = trimmedBody;

            try {
                sendEmailWithAttachment(payslip, finalSubject, finalBody);
                payslip.setStatus(PayslipStatus.SENT);
                payslip.setSentAt(java.time.LocalDateTime.now());
                payslip.setSentTo(payslip.getEmployee().getEmail());
                payslip.setLastSubject(finalSubject);
                payslip.setLastBody(finalBody);
                payslip.setLastError(null);
                result.setSent(result.getSent() + 1);
            } catch (MessagingException | IOException | MailException ex) {
                log.error("Errore durante l'invio del cedolino {}", payslip.getFiscalCode(), ex);
                payslip.setStatus(PayslipStatus.FAILED);
                payslip.setLastError(ex.getMessage());
                result.addError("Errore inviando il cedolino " + payslip.getFiscalCode() + ": " + ex.getMessage());
            }
        }

        return result;
    }

    private void sendEmailWithAttachment(Payslip payslip, String subject, String body)
            throws MessagingException, IOException {
        Path path = Path.of(payslip.getStoragePath());
        String filename = path.getFileName().toString();
        String subdir = path.getParent() == null ? "" : path.getParent().toString();
        Resource resource = fileSystemStorageService.loadAsResource(filename, subdir);
        String attachmentName = StringUtils.hasText(payslip.getOriginalFilename())
                ? payslip.getOriginalFilename()
                : resource.getFilename();
        
        MimeMessagePreparator preparator = mimeMessage -> {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress);
            helper.setTo(payslip.getEmployee().getEmail());
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
            log.info("Invio cedolino riuscito usando fallback SSL su porta 465 per host {}", fallbackSender.getHost());
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

        properties.put("mail.smtp.starttls.enable", "false");
        properties.put("mail.smtp.starttls.required", "false");
        properties.put("mail.smtp.ssl.enable", "true");
        properties.putIfAbsent("mail.smtp.ssl.trust", primarySender.getHost());
        properties.put("mail.smtp.socketFactory.port", String.valueOf(465));
        properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        properties.putIfAbsent("mail.smtp.connectiontimeout", "10000");
        properties.putIfAbsent("mail.smtp.timeout", "10000");
        properties.putIfAbsent("mail.smtp.writetimeout", "10000");

        fallback.setJavaMailProperties(properties);
        return fallback;
    }

    private void applyFallbackConfiguration(JavaMailSenderImpl primarySender, JavaMailSenderImpl fallbackSender) {
        primarySender.setPort(fallbackSender.getPort());
        primarySender.setProtocol(fallbackSender.getProtocol());
        primarySender.setDefaultEncoding(fallbackSender.getDefaultEncoding());
        primarySender.setJavaMailProperties(copyProperties(fallbackSender.getJavaMailProperties()));
        primarySender.setSession((Session) null);
    }

    private Properties copyProperties(Properties source) {
        Properties copy = new Properties();
        if (source != null) {
            copy.putAll(source);
        }
        return copy;
        
    }
}
