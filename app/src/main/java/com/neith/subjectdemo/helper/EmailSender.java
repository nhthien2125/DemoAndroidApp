package com.neith.subjectdemo.helper;

import java.io.File;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class EmailSender {

    private static final String FROM_EMAIL = "httbworkstation@gmail.com";
    private static final String APP_PASSWORD = "cotu wurg gbve crbk";
    private static final String BRAND_NAME = "TBT Center";

    public static void sendOTP(String toEmail, String otp) throws Exception {
        Session session = createMailSession();

        String subject = "[TBT Center] Mã OTP đăng ký tài khoản";

        String htmlBody =
                "<!DOCTYPE html>" +
                        "<html>" +
                        "<head><meta charset='UTF-8'></head>" +
                        "<body style='margin:0;padding:0;background:#0f172a;font-family:Arial,Helvetica,sans-serif;'>" +
                        "<div style='max-width:620px;margin:0 auto;padding:28px 16px;'>" +
                        "<div style='background:#020617;border:1px solid #1f2937;border-radius:18px;overflow:hidden;box-shadow:0 18px 45px rgba(0,0,0,0.35);'>" +
                        "<div style='background:linear-gradient(135deg,#FFD700,#f59e0b);padding:22px 26px;'>" +
                        "<h1 style='margin:0;color:#111827;font-size:24px;font-weight:800;'>TBT Center</h1>" +
                        "<p style='margin:6px 0 0;color:#111827;font-size:14px;font-weight:600;'>Xác thực tài khoản</p>" +
                        "</div>" +
                        "<div style='padding:28px 26px;color:#e5e7eb;'>" +
                        "<h2 style='margin:0 0 12px;font-size:20px;color:#ffffff;'>Mã OTP của bạn</h2>" +
                        "<p style='margin:0 0 18px;color:#cbd5e1;font-size:15px;line-height:1.6;'>Vui lòng sử dụng mã OTP bên dưới để hoàn tất quá trình đăng ký tài khoản.</p>" +
                        "<div style='margin:24px 0;text-align:center;'>" +
                        "<div style='display:inline-block;background:#111827;border:1px solid #FFD700;border-radius:14px;padding:18px 28px;'>" +
                        "<span style='letter-spacing:8px;color:#FFD700;font-size:34px;font-weight:800;'>" + escapeHtml(otp) + "</span>" +
                        "</div>" +
                        "</div>" +
                        "<p style='margin:0;color:#94a3b8;font-size:13px;line-height:1.6;'>Vì lý do bảo mật, vui lòng không chia sẻ mã này cho bất kỳ ai.</p>" +
                        "</div>" +
                        "<div style='border-top:1px solid #1f2937;padding:16px 26px;color:#64748b;font-size:12px;'>Email này được gửi tự động từ hệ thống " + BRAND_NAME + ".</div>" +
                        "</div>" +
                        "</div>" +
                        "</body>" +
                        "</html>";

        sendHtmlMail(session, toEmail, subject, htmlBody);
    }

    public static void sendInterviewEmail(
            String toEmail,
            String candidateName,
            String interviewDate,
            String interviewTime,
            String note
    ) throws Exception {
        Session session = createMailSession();

        String subject = "[TBT Center] Thư mời phỏng vấn";

        String safeName = escapeHtml(candidateName);
        String safeDate = escapeHtml(interviewDate);
        String safeTime = escapeHtml(interviewTime);
        String safeNote = note == null ? "" : escapeHtml(note.trim());

        String noteBlock = "";

        if (!safeNote.isEmpty()) {
            noteBlock =
                    "<div style='margin-top:16px;background:#111827;border-left:4px solid #FFD700;border-radius:12px;padding:14px 16px;'>" +
                            "<div style='color:#FFD700;font-weight:700;font-size:14px;margin-bottom:6px;'>Ghi chú</div>" +
                            "<div style='color:#e5e7eb;font-size:14px;line-height:1.6;'>" + safeNote + "</div>" +
                            "</div>";
        }

        String htmlBody =
                "<!DOCTYPE html>" +
                        "<html>" +
                        "<head><meta charset='UTF-8'></head>" +
                        "<body style='margin:0;padding:0;background:#0f172a;font-family:Arial,Helvetica,sans-serif;'>" +
                        "<div style='max-width:680px;margin:0 auto;padding:28px 16px;'>" +
                        "<div style='background:#020617;border:1px solid #1f2937;border-radius:20px;overflow:hidden;box-shadow:0 18px 45px rgba(0,0,0,0.35);'>" +
                        "<div style='background:linear-gradient(135deg,#FFD700,#f59e0b);padding:24px 28px;'>" +
                        "<h1 style='margin:0;color:#111827;font-size:25px;font-weight:800;'>TBT Center</h1>" +
                        "<p style='margin:7px 0 0;color:#111827;font-size:14px;font-weight:600;'>Thư mời phỏng vấn</p>" +
                        "</div>" +
                        "<div style='padding:30px 28px;color:#e5e7eb;'>" +
                        "<h2 style='margin:0 0 14px;font-size:22px;color:#ffffff;'>Xin chào " + safeName + ",</h2>" +
                        "<p style='margin:0 0 18px;color:#cbd5e1;font-size:15px;line-height:1.7;'>Cảm ơn bạn đã quan tâm và ứng tuyển vào <strong style='color:#FFD700;'>TBT Center</strong>. Chúng tôi trân trọng mời bạn tham gia buổi phỏng vấn với thông tin như sau:</p>" +
                        "<div style='background:#111827;border:1px solid #334155;border-radius:16px;padding:18px 18px;margin:22px 0;'>" +
                        "<table style='width:100%;border-collapse:collapse;'>" +
                        "<tr><td style='padding:10px 0;color:#94a3b8;font-size:14px;width:150px;'>Ứng viên</td><td style='padding:10px 0;color:#ffffff;font-size:15px;font-weight:700;'>" + safeName + "</td></tr>" +
                        "<tr><td style='padding:10px 0;color:#94a3b8;font-size:14px;'>Ngày phỏng vấn</td><td style='padding:10px 0;color:#FFD700;font-size:15px;font-weight:800;'>" + safeDate + "</td></tr>" +
                        "<tr><td style='padding:10px 0;color:#94a3b8;font-size:14px;'>Giờ phỏng vấn</td><td style='padding:10px 0;color:#FFD700;font-size:15px;font-weight:800;'>" + safeTime + "</td></tr>" +
                        "</table>" +
                        "</div>" +
                        noteBlock +
                        "<p style='margin:22px 0 0;color:#cbd5e1;font-size:15px;line-height:1.7;'>Vui lòng phản hồi email này nếu bạn cần thay đổi thời gian phỏng vấn hoặc cần thêm thông tin trước buổi phỏng vấn.</p>" +
                        "<p style='margin:24px 0 0;color:#e5e7eb;font-size:15px;line-height:1.7;'>Trân trọng,<br><strong style='color:#FFD700;'>TBT Center</strong></p>" +
                        "</div>" +
                        "<div style='border-top:1px solid #1f2937;padding:16px 28px;color:#64748b;font-size:12px;line-height:1.5;'>Email này được gửi tự động từ hệ thống tuyển dụng " + BRAND_NAME + ".</div>" +
                        "</div>" +
                        "</div>" +
                        "</body>" +
                        "</html>";

        sendHtmlMail(session, toEmail, subject, htmlBody);
    }

    public static void sendApplicationReceivedEmail(
            String toEmail,
            String candidateName,
            String infoFileName,
            String degreeFileName,
            String otherFileName,
            String submitTime,
            File infoFile,
            File degreeFile,
            File otherFile
    ) throws Exception {
        Session session = createMailSession();

        String subject = "TBT Center HR - Application received";

        String safeName = escapeHtml(candidateName);
        String safeEmail = escapeHtml(toEmail);
        String safeInfo = escapeHtml(infoFileName);
        String safeDegree = escapeHtml(degreeFileName);
        String safeOther = escapeHtml(otherFileName);
        String safeTime = escapeHtml(submitTime);

        String otherLi = safeOther.isEmpty() ? "" : "<li>Others: " + safeOther + "</li>";

        String htmlBody =
                "<!DOCTYPE html>" +
                        "<html>" +
                        "<head><meta charset='UTF-8'></head>" +
                        "<body style='font-family:Segoe UI,Arial,sans-serif;background:#0f172a;margin:0;padding:22px;'>" +
                        "<div style='max-width:620px;margin:auto;background:#020617;color:#f5f5f5;border-radius:18px;overflow:hidden;box-shadow:0 18px 45px rgba(0,0,0,0.4);border:1px solid #1f2937;'>" +
                        "<div style='background:linear-gradient(135deg,#FFD700,#f59e0b);padding:22px 26px;'>" +
                        "<h2 style='margin:0;color:#111827;font-size:25px;font-weight:800;'>TBT Center</h2>" +
                        "<p style='margin:5px 0 0;font-size:13px;color:#111827;font-weight:600;'>Application confirmation</p>" +
                        "</div>" +
                        "<div style='padding:28px 30px;'>" +
                        "<p style='font-size:15px;line-height:1.7;margin-top:0;color:#e5e7eb;'>Dear <b style='color:#FFD700;'>" + safeName + "</b>,<br>We have received your application. Below is a quick summary:</p>" +
                        "<div style='margin:18px 0 16px;padding:14px 16px;border-radius:14px;background:#111827;border:1px solid rgba(148,163,184,0.55);'>" +
                        "<p style='margin:0;font-size:14px;color:#e5e7eb;'><strong>Name:</strong> " + safeName + "</p>" +
                        "<p style='margin:6px 0;font-size:14px;color:#e5e7eb;'><strong>Email:</strong> " + safeEmail + "</p>" +
                        "<p style='margin:6px 0 0;font-size:14px;color:#e5e7eb;'><strong>Time:</strong> " + safeTime + "</p>" +
                        "</div>" +
                        "<p style='margin:0 0 8px;font-size:13px;color:#FFD700;font-weight:700;'>Files sent to us:</p>" +
                        "<div style='margin-bottom:18px;padding:12px 16px;border-radius:12px;background:#111827;border:1px dashed rgba(249,193,42,0.7);'>" +
                        "<ul style='margin:0;padding-left:18px;font-size:13px;color:#e5e7eb;line-height:1.7;'>" +
                        "<li>Personal Information: " + safeInfo + "</li>" +
                        "<li>Degree: " + safeDegree + "</li>" +
                        otherLi +
                        "</ul>" +
                        "<p style='margin:10px 0 0;font-size:12px;color:#9ca3af;'>These files are also attached to this email for your reference.</p>" +
                        "</div>" +
                        "<p style='font-size:13px;line-height:1.7;margin:0;color:#d1d5db;'>Our team will review your application and contact you as soon as possible.<br>Best regards,<br><span style='color:#FFD700;font-weight:700;'>TBT Center HR</span></p>" +
                        "</div>" +
                        "<div style='padding:14px 22px;border-top:1px solid #1f2937;font-size:11px;color:#6b7280;background:#020617;'>© TBT Center. All rights reserved.</div>" +
                        "</div>" +
                        "</body>" +
                        "</html>";

        sendHtmlMailWithAttachments(session, toEmail, subject, htmlBody, infoFile, degreeFile, otherFile);
    }

    private static Session createMailSession() {
        Properties props = buildMailProperties();

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, APP_PASSWORD);
            }
        });
    }

    private static Properties buildMailProperties() {
        Properties props = new Properties();

        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        return props;
    }

    private static void sendHtmlMail(
            Session session,
            String toEmail,
            String subject,
            String htmlBody
    ) throws Exception {
        Message message = new MimeMessage(session);

        message.setFrom(new InternetAddress(FROM_EMAIL, BRAND_NAME));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject(subject);
        message.setContent(htmlBody, "text/html; charset=UTF-8");

        Transport.send(message);
    }

    private static void sendHtmlMailWithAttachments(
            Session session,
            String toEmail,
            String subject,
            String htmlBody,
            File file1,
            File file2,
            File file3
    ) throws Exception {
        MimeMessage message = new MimeMessage(session);

        message.setFrom(new InternetAddress(FROM_EMAIL, BRAND_NAME));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject(subject);

        Multipart multipart = new MimeMultipart();

        BodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(htmlBody, "text/html; charset=UTF-8");
        multipart.addBodyPart(htmlPart);

        addAttachmentIfExists(multipart, file1);
        addAttachmentIfExists(multipart, file2);
        addAttachmentIfExists(multipart, file3);

        message.setContent(multipart);

        Transport.send(message);
    }

    private static void addAttachmentIfExists(Multipart multipart, File file) throws Exception {
        if (file == null || !file.exists()) {
            return;
        }

        MimeBodyPart attachmentPart = new MimeBodyPart();
        DataSource source = new FileDataSource(file);

        attachmentPart.setDataHandler(new DataHandler(source));
        attachmentPart.setFileName(file.getName());

        multipart.addBodyPart(attachmentPart);
    }

    private static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }

        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
                .replace("\n", "<br>");
    }
}