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
 * Sends transactional emails via SMTP.
 *
 * <p>Covered flows:
 * <ul>
 *   <li>Password-reset OTP (existing, user-initiated).</li>
 *   <li>Driver account approval notification (admin approves a driver).</li>
 *   <li>Vehicle approval notification (admin approves a passenger's vehicle).</li>
 * </ul>
 *
 * <p>Email sending is guarded by the {@code app.mail.enabled} flag.
 * {@link JavaMailSender} is injected as optional so the backend still starts
 * up fine when SMTP credentials are not configured (dev mode). Delivery
 * failures for <em>notifications</em> are logged but never rethrown — a flaky
 * SMTP provider must not break the admin approval workflow.
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

    // ── Password reset ────────────────────────────────────────────────────────

    /**
     * Sends a password-reset OTP email to {@code toEmail}.
     * No-op when email is disabled.
     *
     * <p>Unlike the approval notifications, this method <strong>does</strong>
     * rethrow on SMTP failure: the caller (password-reset flow) needs to know
     * when delivery failed so it can surface a clear error to the user.
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
            helper.setText(buildOtpHtmlBody(otpCode), true);

            mailSender.send(message);
            log.info("Password-reset OTP sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send verification email: " + e.getMessage(), e);
        }
    }

    // ── Approval notifications ───────────────────────────────────────────────

    /**
     * Notifies a driver that their account has been verified (approved).
     *
     * <p>Fired when an admin moves a driver from {@code PENDING}/{@code REJECTED}
     * to {@code VERIFIED}. Delivery failures are <strong>swallowed and logged</strong>
     * so the admin's approval call always succeeds — the status change is the
     * source of truth; the email is a best-effort heads-up.
     *
     * @param toEmail  recipient e-mail address (driver's login email)
     * @param fullName recipient's display name, used in the greeting (may be null/blank)
     */
    public void sendDriverApprovedNotification(String toEmail, String fullName) {
        if (!mailEnabled || mailSender == null) {
            log.debug("Email disabled — skipping driver-approved notification to {}", toEmail);
            return;
        }
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Skipping driver-approved notification: missing recipient email");
            return;
        }

        sendSimpleHtml(
                toEmail,
                "DriveMe — Your driver account has been approved",
                buildDriverApprovedBody(safeName(fullName))
        );
    }

    /**
     * Notifies a passenger that one of their vehicles has been verified
     * (approved for ride requests).
     *
     * @param toEmail       recipient e-mail (vehicle owner's login email)
     * @param fullName      recipient's display name (may be null/blank)
     * @param vehicleLabel  human-readable vehicle description (e.g. "Toyota Corolla")
     * @param plateNumber   vehicle plate number (may be null/blank)
     */
    public void sendVehicleApprovedNotification(
            String toEmail, String fullName, String vehicleLabel, String plateNumber) {
        if (!mailEnabled || mailSender == null) {
            log.debug("Email disabled — skipping vehicle-approved notification to {}", toEmail);
            return;
        }
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Skipping vehicle-approved notification: missing recipient email");
            return;
        }

        String safeVehicle = (vehicleLabel == null || vehicleLabel.isBlank())
                ? "your vehicle"
                : vehicleLabel;
        String safePlate = (plateNumber == null || plateNumber.isBlank())
                ? ""
                : plateNumber;

        sendSimpleHtml(
                toEmail,
                "DriveMe — Your vehicle has been approved",
                buildVehicleApprovedBody(safeName(fullName), safeVehicle, safePlate)
        );
    }

    // ── Internal send helper ─────────────────────────────────────────────────

    /**
     * Builds and sends a single HTML e-mail. Failures are logged, never
     * rethrown — this is only used by best-effort notifications.
     */
    private void sendSimpleHtml(String toEmail, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("Notification email sent to {} (subject: {})", toEmail, subject);
        } catch (Exception e) {
            // Best-effort delivery: log + continue.
            log.error("Failed to send notification email to {} (subject: {}): {}",
                    toEmail, subject, e.getMessage());
        }
    }

    private static String safeName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "there";
        }
        return fullName.trim();
    }

    // ── HTML templates ────────────────────────────────────────────────────────

    /**
     * Shell template used by all approval notifications. Keeps the dark-theme
     * branding consistent across mails and avoids copy-pasting 40 lines of
     * table markup for every new notification type.
     *
     * @param headerSubtitle small grey label below the "DriveMe" title
     * @param bodyHtml       inner body HTML (shown inside the card)
     */
    private String buildNotificationShell(String headerSubtitle, String bodyHtml) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8"/>
                  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
                  <title>DriveMe</title>
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
                                %s
                              </p>
                            </td>
                          </tr>

                          <!-- Body -->
                          <tr>
                            <td style="padding:28px 32px 32px;color:#D1D5DB;font-size:14px;line-height:1.6;">
                              %s
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
                """.formatted(escapeHtml(headerSubtitle), bodyHtml);
    }

    private String buildDriverApprovedBody(String fullName) {
        String body = """
                <p style="margin:0 0 12px;">Hi %s,</p>

                <p style="margin:0 0 16px;">
                  Great news — your DriveMe <strong style="color:#FFFFFF;">driver account</strong>
                  has been reviewed and <strong style="color:#34D399;">approved</strong>. You can
                  now sign in and start accepting trip requests right away.
                </p>

                <div style="background:#0B1220;border:1px solid #064E3B;border-radius:12px;
                            padding:16px;margin:16px 0;">
                  <p style="margin:0;color:#34D399;font-size:13px;font-weight:600;">
                    Account status: Verified
                  </p>
                  <p style="margin:6px 0 0;color:#9CA3AF;font-size:12px;">
                    Open the app and head to the landing screen to go online.
                  </p>
                </div>

                <p style="margin:0;color:#9CA3AF;font-size:12px;">
                  Thanks for joining DriveMe. Drive safely and good luck on your first trips!
                </p>
                """.formatted(escapeHtml(fullName));

        return buildNotificationShell("Driver account approved", body);
    }

    private String buildVehicleApprovedBody(String fullName, String vehicleLabel, String plate) {
        String plateRow = plate.isEmpty() ? "" : """
                  <p style="margin:6px 0 0;color:#9CA3AF;font-size:12px;">
                    Plate: <span style="color:#FFFFFF;font-weight:600;">%s</span>
                  </p>
                """.formatted(escapeHtml(plate));

        String body = """
                <p style="margin:0 0 12px;">Hi %s,</p>

                <p style="margin:0 0 16px;">
                  Your vehicle has been reviewed and
                  <strong style="color:#34D399;">approved</strong>. You can now select it when
                  requesting a ride on DriveMe.
                </p>

                <div style="background:#0B1220;border:1px solid #1E3A5F;border-radius:12px;
                            padding:16px;margin:16px 0;">
                  <p style="margin:0;color:#FFFFFF;font-size:14px;font-weight:600;">
                    %s
                  </p>
                  %s
                  <p style="margin:8px 0 0;color:#34D399;font-size:12px;font-weight:600;">
                    Status: Verified
                  </p>
                </div>

                <p style="margin:0;color:#9CA3AF;font-size:12px;">
                  Open the app and go to the home screen to request your next ride.
                </p>
                """.formatted(escapeHtml(fullName), escapeHtml(vehicleLabel), plateRow);

        return buildNotificationShell("Vehicle approved", body);
    }

    private String buildOtpHtmlBody(String code) {
        String body = """
                <p style="margin:0 0 16px;">
                  We received a request to reset the password for your DriveMe account.
                  Use the code below to complete the process. It expires in
                  <strong style="color:#FFFFFF;">10 minutes</strong>.
                </p>

                <div style="text-align:center;padding:8px 0 20px;">
                  <div style="display:inline-block;
                              background:#1E293B;
                              border:1px solid #06B6D4;
                              border-radius:12px;
                              padding:20px 40px;">
                    <p style="margin:0;
                               font-size:36px;
                               font-weight:700;
                               letter-spacing:12px;
                               color:#FFFFFF;">%s</p>
                  </div>
                </div>

                <p style="margin:0;color:#9CA3AF;font-size:12px;line-height:1.5;">
                  If you did not request a password reset, you can safely ignore this email.
                  Your password will not change.
                </p>
                """.formatted(escapeHtml(code));

        return buildNotificationShell("Password Reset Request", body);
    }

    /**
     * Minimal HTML escaper for values interpolated into our mail templates.
     * Prevents a malicious / unlucky full name or vehicle label from breaking
     * the markup or injecting attributes.
     */
    private static String escapeHtml(String raw) {
        if (raw == null) return "";
        return raw.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }
}
