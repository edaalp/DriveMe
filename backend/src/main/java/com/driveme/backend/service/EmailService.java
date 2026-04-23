package com.driveme.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

/**
 * Sends transactional emails (e.g. password-reset OTP) via SMTP.
 *
 * <p>Email sending is guarded by the {@code app.mail.enabled} flag.
 * {@code JavaMailSender} is injected as optional so the backend starts up
 * fine even when SMTP credentials are not configured (dev mode).
 */
@Service
@Slf4j
public class EmailService {

    /**
     * May be {@code null} when Spring Mail is not configured
     * (i.e. {@code spring.mail.host} / username / password are missing).
     */
    @Nullable
    private final JavaMailSender mailSender;

    private final boolean mailEnabled;
    private final String  fromAddress;

    /**
     * {@code JavaMailSender} is optional so the application starts correctly
     * even when SMTP credentials have not been supplied.
     */
    public EmailService(
            @Autowired(required = false) @Nullable JavaMailSender mailSender,
            @Value("${app.mail.enabled:false}") boolean mailEnabled,
            @Value("${app.mail.from:noreply@driveme.com}") String fromAddress) {

        this.mailSender  = mailSender;
        // Force-disable if the sender bean isn't available regardless of the flag.
        this.mailEnabled = mailEnabled && mailSender != null;
        this.fromAddress = fromAddress;

        if (mailEnabled && mailSender == null) {
            log.warn("app.mail.enabled=true but JavaMailSender is not configured. "
                   + "Check spring.mail.* properties. Falling back to dev mode (OTP in response).");
        }
    }

    /** Returns {@code true} when SMTP sending is active and a sender bean is available. */
    public boolean isEnabled() {
        return mailEnabled;
    }

    /**
     * Sends a password-reset OTP email to {@code toEmail}.
     * No-op when email is disabled.
     */
    public void sendPasswordResetOtp(String toEmail, String otpCode) {
        if (!mailEnabled || mailSender == null) {
            log.debug("Email disabled — skipping OTP delivery to {}", toEmail);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("DriveMe — Your Password Reset Code");
            helper.setText(buildHtmlBody(otpCode), true);

            mailSender.send(message);
            log.info("Password-reset OTP sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send verification email: " + e.getMessage(), e);
        }
    }

    // ── HTML template ─────────────────────────────────────────────────────────

    private String buildHtmlBody(String code) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8"/>
                  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
                  <title>Password Reset</title>
                </head>
                <body style="margin:0;padding:0;background:#0B1220;font-family:sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#0B1220;padding:40px 0;">
                    <tr>
                      <td align="center">
                        <table width="480" cellpadding="0" cellspacing="0"
                               style="background:#0F172A;border-radius:16px;
                                      border:1px solid #1F2937;overflow:hidden;">

                          <!-- Header gradient bar -->
                          <tr>
                            <td style="background:linear-gradient(90deg,#06B6D4,#2563EB);
                                        height:4px;font-size:0;">&nbsp;</td>
                          </tr>

                          <!-- Logo / title -->
                          <tr>
                            <td align="center" style="padding:32px 32px 0;">
                              <p style="margin:0;font-size:24px;font-weight:700;color:#FFFFFF;">
                                DriveMe
                              </p>
                              <p style="margin:8px 0 0;font-size:14px;color:#9CA3AF;">
                                Password Reset Request
                              </p>
                            </td>
                          </tr>

                          <!-- Body -->
                          <tr>
                            <td style="padding:28px 32px 0;color:#D1D5DB;font-size:14px;line-height:1.6;">
                              <p style="margin:0;">
                                We received a request to reset the password for your DriveMe account.
                                Use the code below to complete the process. It expires in
                                <strong style="color:#FFFFFF;">10 minutes</strong>.
                              </p>
                            </td>
                          </tr>

                          <!-- OTP code box -->
                          <tr>
                            <td align="center" style="padding:28px 32px;">
                              <div style="display:inline-block;
                                          background:#1E293B;
                                          border:1px solid #06B6D4;
                                          border-radius:12px;
                                          padding:20px 40px;">
                                <p style="margin:0;
                                           font-size:36px;
                                           font-weight:700;
                                           letter-spacing:12px;
                                           color:#000000;">%s</p>
                              </div>
                            </td>
                          </tr>

                          <!-- Warning -->
                          <tr>
                            <td style="padding:0 32px 28px;color:#9CA3AF;font-size:12px;line-height:1.5;">
                              <p style="margin:0;">
                                If you did not request a password reset, you can safely ignore this email.
                                Your password will not change.
                              </p>
                            </td>
                          </tr>

                          <!-- Footer -->
                          <tr>
                            <td style="padding:16px 32px;
                                        border-top:1px solid #1F2937;
                                        color:#6B7280;font-size:11px;
                                        text-align:center;">
                              &copy; 2026 DriveMe. This is an automated message — please do not reply.
                            </td>
                          </tr>

                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(code);
    }
}
