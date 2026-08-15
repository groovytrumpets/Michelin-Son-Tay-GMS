package com.g42.platform.gms.notification.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Cấu hình SMTP gửi email.
 *
 * Tự tạo bean {@link JavaMailSender} thay vì để Spring Boot tự cấu hình, vì cần
 * XOÁ DẤU CÁCH trong mật khẩu: Google hiển thị App Password thành 4 nhóm 4 ký tự
 * ("abcd efgh ijkl mnop") nên ai cũng copy kèm dấu cách, mà SMTP thì gửi nguyên chuỗi
 * đó đi và Gmail trả về "Authentication failed" rất khó đoán nguyên nhân.
 */
@Configuration
public class MailConfig {

    private static final Logger log = LoggerFactory.getLogger(MailConfig.class);

    @Value("${notification.email.host:}")
    private String host;

    @Value("${notification.email.port:587}")
    private int port;

    @Value("${notification.email.username:}")
    private String username;

    @Value("${notification.email.password:}")
    private String password;

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host == null ? "" : host.trim());
        sender.setPort(port);
        sender.setUsername(username == null ? "" : username.trim());
        // App Password của Google được hiển thị kèm dấu cách nhưng phải dùng ở dạng liền
        sender.setPassword(password == null ? "" : password.replaceAll("\\s", ""));
        sender.setDefaultEncoding("UTF-8");

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");

        if (host == null || host.isBlank()) {
            log.warn("Email chưa bật: thiếu notification.email.host (env MAIL_SMTP_HOST).");
        } else if (password == null || password.isBlank()) {
            log.warn("Email chưa bật: thiếu notification.email.password (env MAIL_SMTP_PASSWORD).");
        } else {
            // In ra ĐỘ DÀI mật khẩu (không in giá trị) để soi nhanh khi Gmail báo sai thông tin
            // đăng nhập: App Password của Google luôn đúng 16 ký tự sau khi bỏ dấu cách.
            // Lệch 16 nghĩa là đang lấy nhầm nguồn — biến môi trường MAIL_SMTP_PASSWORD
            // luôn thắng giá trị mặc định viết trong application.properties.
            String effectivePassword = sender.getPassword();
            log.info("Email đã bật: {}:{}, tài khoản {}, mật khẩu {} ký tự{}",
                    sender.getHost(), port, sender.getUsername(), effectivePassword.length(),
                    effectivePassword.length() == 16 ? "" : " (BẤT THƯỜNG — App Password phải đúng 16 ký tự)");
        }
        return sender;
    }
}
