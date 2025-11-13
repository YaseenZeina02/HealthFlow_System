package com.example.healthflow.core.email;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.search.FlagTerm;
import java.util.Properties;
import java.util.regex.*;
import com.example.healthflow.db.Database;
import com.example.healthflow.dao.UserDAO;
import com.example.healthflow.core.auth.ResetUtil;
import org.mindrot.jbcrypt.BCrypt;

public final class AdminMailWatcher {

    private final String imapHost, imapUser, imapPass;
    private final Properties props = new Properties();
    private volatile boolean running;
    private static volatile boolean STARTED = false;

    String tempPass = ResetUtil.generateOtp6(); // أو مولّدك الحالي (8–12 chars أفضل)

    public AdminMailWatcher(String host, String user, String pass, boolean ssl) {
        this.imapHost = host; this.imapUser = user; this.imapPass = pass;
        props.put("mail.store.protocol", ssl ? "imaps" : "imap");
    }

    public void start(java.util.function.Consumer<String> log) {
        running = true;
        Thread t = new Thread(() -> {
            while (running) {
                try { pollOnce(log); } catch (Exception ex) {
                    if (log != null) log.accept("Mail poll err: " + ex.getMessage());
                }
                try { Thread.sleep(60_000); } catch (InterruptedException ignored) {}
            }
        }, "AdminMailWatcher");
        t.setDaemon(true);
        t.start();
    }

    public void stop() { running = false; }

    private void pollOnce(java.util.function.Consumer<String> log) throws Exception {
        var session = Session.getInstance(props);
        try (var store = session.getStore()) {
            store.connect(imapHost, imapUser, imapPass);
            try (var folder = store.getFolder("INBOX")) {
                folder.open(Folder.READ_WRITE);
                Message[] msgs = folder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));

                for (Message m : msgs) {
                    String subj = m.getSubject() == null ? "" : m.getSubject().trim();
                    String body = getBodyText(m).trim();
                    String cmd = findCommand(subj + "\n" + body);
                    if (cmd == null) continue;

                    if (log != null) log.accept("Admin cmd: " + cmd);

                    boolean approve = cmd.startsWith("APPROVE");
                    String email = cmd.replaceFirst("(?i)^(APPROVE|REJECT)\\s+", "").trim();

                    Long userId = findUserIdByEmail(email);
                    if (userId == null) {
                        markDone(m, "[INVALID EMAIL] ");
                        continue;
                    }

                    String otp = null;
                    if (approve) {
                        // 1) أنشئ كلمة مرور مؤقتة قوية
                        String tempPass = com.example.healthflow.core.auth.ResetUtil.generateTempPassword(12);
                        String bcrypt   = BCrypt.hashpw(tempPass, BCrypt.gensalt(12));

                        // 2) حدّث كلمة السر مباشرة في الداتابيز
                        try (var c = Database.get();
                             var ps = c.prepareStatement("UPDATE users SET password_hash=? WHERE id=?")) {
                            ps.setString(1, bcrypt);
                            ps.setLong(2, userId);
                            ps.executeUpdate();
                        }

                        // 3) أرسل الكلمة المؤقتة للمستخدم مع تعليمات تغييرها بعد تسجيل الدخول
                        String htmlBodyForUser = """
                            <h3>Password reset approved</h3>
                            <p>Your temporary password is:</p>
                            <p style="font-size:18px"><b>%s</b></p>
                            <p>Please sign in and change it immediately from your profile.</p>
                            <p>If you did not request this change, contact the administrator.</p>
                            """.formatted(tempPass);

                        sendHtmlEmail(
                                "smtp.gmail.com", 587, true,
                                imapUser, imapPass,
                                email,
                                "Your HealthFlow temporary password",
                                htmlBodyForUser
                        );
                        markDone(m, "[APPROVED] ");
                    } else {
                        sendHtmlEmail(
                                "smtp.gmail.com", 587, true,
                                imapUser, imapPass,
                                email,
                                "Reset request declined",
                                "<p>Your password reset request was declined by the administrator.</p>"
                        );
                        markDone(m, "[REJECTED] ");
                    }
                }
            }
        }
    }

    private static void markDone(Message m, String prefix) throws Exception {
        try { m.setFlag(Flags.Flag.SEEN, true); } catch (Exception ignored) {}
        try { m.setSubject(prefix + (m.getSubject()==null ? "" : m.getSubject())); } catch (Exception ignored) {}
    }

    private static String getBodyText(Message m) throws Exception {
        if (m.isMimeType("text/*")) return (String) m.getContent();
        if (m.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) m.getContent();
            for (int i=0;i<mp.getCount();i++) {
                var p = mp.getBodyPart(i);
                if (p.isMimeType("text/plain")) return (String) p.getContent();
                if (p.isMimeType("text/html")) return stripHtml((String)p.getContent());
            }
        }
        return "";
    }

    private static String stripHtml(String html) { return html.replaceAll("<[^>]+>", " "); }

    private static String findCommand(String s) {
        Matcher mt = Pattern
                .compile("(?i)\\b(APPROVE|REJECT)\\s+([A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,})\\b")
                .matcher(s);
        return mt.find() ? (mt.group(1).toUpperCase() + " " + mt.group(2)) : null;
    }

    private static Long findUserIdByEmail(String email) {
        if (email == null || email.isBlank()) return null;
        try (var c = Database.get();
             var ps = c.prepareStatement("SELECT id FROM users WHERE lower(email)=lower(?) LIMIT 1")) {
            ps.setString(1, email.trim());
            try (var rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong("id");
            }
        } catch (Exception ex) {
            System.err.println("[AdminMailWatcher] DB lookup error: " + ex.getMessage());
        }
        return null;
    }

    /**
     * Send a basic HTML email using Jakarta Mail directly.
     */
    private static void sendHtmlEmail(String smtpHost, int smtpPort, boolean startTls,
                                      String fromUser, String fromPass,
                                      String to, String subject, String htmlBody) {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", startTls ? "true" : "false");
            props.put("mail.smtp.ssl.enable", String.valueOf(!startTls));
            props.put("mail.smtp.host", smtpHost);
            props.put("mail.smtp.port", String.valueOf(smtpPort));
            if (Boolean.parseBoolean(System.getenv().getOrDefault("HF_MAIL_DEBUG", "false"))) {
                System.setProperty("mail.debug", "true");
            }

            Session session = Session.getInstance(props, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(fromUser, fromPass);
                }
            });

            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(fromUser, "HealthFlow Admin"));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            msg.setSubject(subject);
            msg.setContent(htmlBody, "text/html; charset=UTF-8");

            Transport.send(msg);
            System.out.println("[AdminMailWatcher] Sent email to " + to);
        } catch (Exception ex) {
            System.err.println("[AdminMailWatcher] Failed to send email: " + ex.getMessage());
        }
    }


    public static synchronized void bootIfEnvPresent() {
        if (STARTED) return;
        String imapUser = System.getenv("HF_SMTP_USER");   // نفس بريد النظام
        String imapPass = System.getenv("HF_SMTP_PASS");   // app password
        if (imapUser == null || imapPass == null) return;

        AdminMailWatcher w = new AdminMailWatcher(
                "imap.gmail.com", imapUser, imapPass, /*useSsl*/ true
        );
        w.start(System.out::println);
        STARTED = true;
    }

}