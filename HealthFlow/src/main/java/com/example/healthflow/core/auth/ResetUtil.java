package com.example.healthflow.core.auth;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.*;
import java.time.*;

public class ResetUtil {
    private static final SecureRandom RNG = new SecureRandom();
    private static final ZoneId APP_TZ = ZoneId.of("UTC");

    /** مولّد OTP من 6 أرقام */
    public static String generateOtp6() {
        int v = RNG.nextInt(1_000_000); // 0..999999
        return String.format("%06d", v);
    }

    /** SHA-256 lower-hex */
    public static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    /** حفظ هاش الـ OTP بمهلة انتهاء */
    public static void storeOtp(Connection c, long userId, String rawOtp, int minutesValid, String info) throws Exception {
        String otpHash = sha256(rawOtp);
        Instant now = Instant.now();
        Timestamp expires = Timestamp.from(now.plus(Duration.ofMinutes(minutesValid)));
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO password_reset_tokens (user_id, otp_hash, expires_at, request_info) VALUES (?,?,?,?)")) {
            ps.setLong(1, userId);
            ps.setString(2, otpHash);
            ps.setTimestamp(3, expires);
            ps.setString(4, info);
            ps.executeUpdate();
        }
    }

    /** توليد كلمة مرور مؤقتة قوية (A–Z,a–z,0–9 و رموز بسيطة) */
    public static String generateTempPassword(int length) {
        final String U = "ABCDEFGHJKLMNPQRSTUVWXYZ";      // بدون I,O
        final String L = "abcdefghijkmnpqrstuvwxyz";      // بدون l,o
        final String D = "23456789";                      // بدون 0,1
        final String S = "@#$%&*+-_";                     // رموز بسيطة آمنة
        final String ALL = U + L + D + S;

        if (length < 8) length = 8;
        StringBuilder sb = new StringBuilder(length);

        // تأكيد التنوع
        java.security.SecureRandom r = RNG;
        sb.append(U.charAt(r.nextInt(U.length())));
        sb.append(L.charAt(r.nextInt(L.length())));
        sb.append(D.charAt(r.nextInt(D.length())));
        sb.append(S.charAt(r.nextInt(S.length())));

        for (int i = 4; i < length; i++) sb.append(ALL.charAt(r.nextInt(ALL.length())));

        // خلط
        for (int i = sb.length() - 1; i > 0; i--) {
            int j = r.nextInt(i + 1);
            char c = sb.charAt(i);
            sb.setCharAt(i, sb.charAt(j));
            sb.setCharAt(j, c);
        }
        return sb.toString();
    }

    /** تحقّق/استهلاك OTP + تحديث كلمة السر */
    public static boolean verifyOtpAndReset(Connection c, long userId, String rawOtp, String newPlain) throws Exception {
        String otpHash = sha256(rawOtp);

        // ابحث عن آخر توكن غير مستخدم ضمن الصلاحية
        String q = """
            SELECT id, expires_at, used
            FROM password_reset_tokens
            WHERE user_id=? AND otp_hash=? 
            ORDER BY id DESC
            LIMIT 1
        """;
        try (PreparedStatement ps = c.prepareStatement(q)) {
            ps.setLong(1, userId);
            ps.setString(2, otpHash);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                if (rs.getBoolean("used")) return false;
                Timestamp exp = rs.getTimestamp("expires_at");
                if (exp == null || exp.toInstant().isBefore(Instant.now())) return false;
                long tokenId = rs.getLong("id");

                // حدّث كلمة السر
                String bcrypt = org.mindrot.jbcrypt.BCrypt.hashpw(newPlain, org.mindrot.jbcrypt.BCrypt.gensalt(12));
                try (PreparedStatement up = c.prepareStatement("UPDATE users SET password_hash=? WHERE id=?")) {
                    up.setString(1, bcrypt);
                    up.setLong(2, userId);
                    up.executeUpdate();
                }
                // علّم الـ token كمستخدم
                try (PreparedStatement up = c.prepareStatement("UPDATE password_reset_tokens SET used=true WHERE id=?")) {
                    up.setLong(1, tokenId);
                    up.executeUpdate();
                }
                return true;
            }
        }
    }
}