//package com.example.healthflow.dao;
//
//import com.example.healthflow.model.Doctor;
//import com.example.healthflow.model.DoctorAvailability;
//import org.jetbrains.annotations.Nullable;
//
//import java.sql.*;
//import java.util.ArrayList;
//import java.util.List;
//
//public class DoctorDAO {
//
//    public Doctor insert(Connection c, Long userId, String specialty, String bio) throws SQLException {
//        final String sql = """
//            INSERT INTO doctors (user_id, specialty, bio)
//            VALUES (?, ?, ?)
//            RETURNING id, user_id, specialty, bio, updated_at
//            """;
//        try (PreparedStatement ps = c.prepareStatement(sql)) {
//            ps.setLong(1, userId);
//            ps.setString(2, specialty);
//            ps.setString(3, bio);
//            try (ResultSet rs = ps.executeQuery()) {
//                if (rs.next()) {
//                    Doctor d = new Doctor();
//                    d.setId(rs.getLong("id"));
//                    d.setUserId(rs.getLong("user_id"));
//                    d.setSpecialty(rs.getString("specialty"));
//                    d.setBio(rs.getString("bio"));
//                    d.setUpdatedAt(rs.getObject("updated_at", java.time.OffsetDateTime.class));
//                    return d;
//                }
//            }
//        }
//        throw new SQLException("Failed to create doctor");
//    }
//
//    public Doctor findById(Connection c, Long id) throws SQLException {
//        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM doctors WHERE id = ?")) {
//            ps.setLong(1, id);
//            try (ResultSet rs = ps.executeQuery()) {
//                if (!rs.next()) return null;
//                Doctor d = new Doctor();
//                d.setId(rs.getLong("id"));
//                d.setUserId(rs.getLong("user_id"));
//                d.setSpecialty(rs.getString("specialty"));
//                d.setBio(rs.getString("bio"));
//                d.setUpdatedAt(rs.getObject("updated_at", java.time.OffsetDateTime.class));
//                return d;
//            }
//        }
//    }
//
//    public List<Doctor> list(Connection c, int limit, int offset) throws SQLException {
//        final String sql = "SELECT * FROM doctors ORDER BY id LIMIT ? OFFSET ?";
//        try (PreparedStatement ps = c.prepareStatement(sql)) {
//            ps.setInt(1, Math.max(1, limit));
//            ps.setInt(2, Math.max(0, offset));
//            try (ResultSet rs = ps.executeQuery()) {
//                List<Doctor> out = new ArrayList<>();
//                while (rs.next()) {
//                    Doctor d = new Doctor();
//                    d.setId(rs.getLong("id"));
//                    d.setUserId(rs.getLong("user_id"));
//                    d.setSpecialty(rs.getString("specialty"));
//                    d.setBio(rs.getString("bio"));
//                    d.setUpdatedAt(rs.getObject("updated_at", java.time.OffsetDateTime.class));
//                    out.add(d);
//                }
//                return out;
//            }
//        }
//    }
//
//    public boolean updateProfile(Connection c, Long id, String specialty, String bio) throws SQLException {
//        final String sql = "UPDATE doctors SET specialty = ?, bio = ?, updated_at = NOW() WHERE id = ?";
//        try (PreparedStatement ps = c.prepareStatement(sql)) {
//            ps.setString(1, specialty);
//            ps.setString(2, bio);
//            ps.setLong(3, id);
//            return ps.executeUpdate() == 1;
//        }
//    }
//    public Doctor findByUserId(Connection c, long userId) throws SQLException {
//        final String sql = "SELECT * FROM doctors WHERE user_id = ?";
//        try (PreparedStatement ps = c.prepareStatement(sql)) {
//            ps.setLong(1, userId);
//            try (ResultSet rs = ps.executeQuery()) {
//                if (!rs.next()) return null;
//                Doctor d = new Doctor();
//                d.setId(rs.getLong("id"));
//                d.setUserId(rs.getLong("user_id"));
//                d.setSpecialty(rs.getString("specialty"));
//                d.setBio(rs.getString("bio"));
//                d.setUpdatedAt(rs.getObject("updated_at", java.time.OffsetDateTime.class));
//                return d;
//            }
//        }
//    }
//
//    public Doctor ensureProfileForUser(Connection c, long userId) throws SQLException {
//        Doctor d = findByUserId(c, userId);
//        if (d != null) return d;
//        // create a minimal profile if missing
//        return insert(c, userId, "GENERAL", null);
//    }
//
//    public DoctorAvailability getAvailability(Connection c, long doctorUserId) throws Exception {
//        String sql = "SELECT availability_status FROM doctor_profiles WHERE user_id=?";
//        try (var ps = c.prepareStatement(sql)) {
//            ps.setLong(1, doctorUserId);
//            try (var rs = ps.executeQuery()) {
//                if (rs.next()) {
//                    String v = rs.getString(1);
//                    return DoctorAvailability.valueOf(v);
//                }
//                throw new IllegalStateException("Doctor profile not found for user " + doctorUserId);
//            }
//        }
//    }
//
//    public void setAvailability(Connection c, long doctorUserId, DoctorAvailability status) throws Exception {
//        String sql = "UPDATE doctor_profiles SET availability_status=? WHERE user_id=?";
//        try (var ps = c.prepareStatement(sql)) {
//            ps.setString(1, status.name());
//            ps.setLong(2, doctorUserId);
//            ps.executeUpdate();
//        }
//    }
//
//    /** قائمة الأطباء المتاحين (اختياري: حسب التخصص) */
//    public List<Long> listAvailableDoctorUserIds(Connection c, @Nullable String specialty) throws Exception {
//        String base = """
//            SELECT dp.user_id
//            FROM doctor_profiles dp
//            %s
//            WHERE dp.availability_status='AVAILABLE'
//            """;
//        String join = (specialty == null) ? "" : "JOIN doctors d ON d.user_id = dp.user_id AND d.specialty = ?";
//        String sql = String.format(base, join);
//
//        try (var ps = c.prepareStatement(sql)) {
//            if (specialty != null) ps.setString(1, specialty);
//            try (var rs = ps.executeQuery()) {
//                List<Long> list = new ArrayList<>();
//                while (rs.next()) list.add(rs.getLong(1));
//                return list;
//            }
//        }
//    }
//}

package com.example.healthflow.dao;

import com.example.healthflow.db.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO للدكاترة مع دعم حالة التوفر + جلب الأطباء المتاحين حسب التخصص.
 * يعتمد على جدول doctors وعمود availability_status من النوع ENUM doctor_status.
 */
public class DoctorDAO {

    // حالة الطبيب (مطابقة لقيم ENUM في قاعدة البيانات)
    public enum DoctorAvailability {
        AVAILABLE, IN_APPOINTMENT, ON_BREAK
    }

    /** عنصر للعرض في ComboBox: يحمل doctor_id و user_id والاسم والتخصص */
    public static final class DoctorOption {
        public final long doctorId;
        public final long userId;
        public final String fullName;
        public final String specialty;

        public DoctorOption(long doctorId, long userId, String fullName, String specialty) {
            this.doctorId = doctorId;
            this.userId = userId;
            this.fullName = fullName;
            this.specialty = specialty;
        }

        @Override
        public String toString() {
            return fullName + " (" + specialty + ")";
        }
    }

    /* ==================== حالة التوفر ==================== */

    public void setAvailability(long doctorUserId, DoctorAvailability status) throws Exception {
        try (Connection c = Database.get()) {
            setAvailability(c, doctorUserId, status);
        }
    }

    public void setAvailability(Connection c, long doctorUserId, DoctorAvailability status) throws Exception {
        String sql = "UPDATE doctors SET availability_status = ? WHERE user_id = ?";
        try (var ps = c.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, doctorUserId);
            ps.executeUpdate();
        }
    }

    public DoctorAvailability getAvailability(long doctorUserId) throws Exception {
        try (Connection c = Database.get()) {
            return getAvailability(c, doctorUserId);
        }
    }

    public DoctorAvailability getAvailability(Connection c, long doctorUserId) throws Exception {
        String sql = "SELECT availability_status FROM doctors WHERE user_id = ?";
        try (var ps = c.prepareStatement(sql)) {
            ps.setLong(1, doctorUserId);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return DoctorAvailability.valueOf(rs.getString(1));
            }
        }
    }

    /* ==================== التخصصات ==================== */

    /** يعيد قائمة بالتخصصات الموجودة */
    public List<String> listSpecialties() throws Exception {
        try (Connection c = Database.get()) {
            return listSpecialties(c);
        }
    }

    public List<String> listSpecialties(Connection c) throws Exception {
        String sql = "SELECT DISTINCT specialty FROM doctors ORDER BY specialty";
        try (var ps = c.prepareStatement(sql);
             var rs = ps.executeQuery()) {
            List<String> list = new ArrayList<>();
            while (rs.next()) list.add(rs.getString(1));
            return list;
        }
    }

    /* ==================== الأطباء المتاحون ==================== */

    /** جميع الأطباء المتاحين (إن مررت specialty=null يعيد كل المتاحين) */
    public List<DoctorOption> listAvailableBySpecialty(String specialty) throws Exception {
        try (Connection c = Database.get()) {
            return listAvailableBySpecialty(c, specialty);
        }
    }



    public List<DoctorOption> listAvailableBySpecialty(Connection c, String specialty) throws Exception {
        String sql = """
            SELECT d.id, d.user_id, u.full_name, d.specialty
            FROM doctors d
            JOIN users u ON u.id = d.user_id
            WHERE d.availability_status = 'AVAILABLE'
              AND (? IS NULL OR d.specialty = ?)
            ORDER BY u.full_name
            """;
        try (var ps = c.prepareStatement(sql)) {
            if (specialty == null || specialty.isBlank()) {
                ps.setNull(1, java.sql.Types.VARCHAR);
                ps.setNull(2, java.sql.Types.VARCHAR);
            } else {
                ps.setString(1, specialty);
                ps.setString(2, specialty);
            }
            try (var rs = ps.executeQuery()) {
                List<DoctorOption> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new DoctorOption(
                            rs.getLong(1),
                            rs.getLong(2),
                            rs.getString(3),
                            rs.getString(4)
                    ));
                }
                return list;
            }
        }
    }
    public void ensureProfileForUser(Connection c, long userId) throws SQLException {
        final String sql = """
            INSERT INTO doctors (user_id, specialty)
            VALUES (?, ?)
            ON CONFLICT (user_id) DO NOTHING
        """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, "GENERAL"); // اختر أي قيمة افتراضية مناسبة عندك
            ps.executeUpdate();
        }
        // لو عندك أعمدة أخرى NOT NULL لازم قيم افتراضية لها، أضفها هنا.
    }

    /**
     * نسخة مريحة بدون تمرير Connection (تفتح/تغلق الاتصال داخليًا).
     */
    public void ensureProfileForUser(long userId) throws SQLException {
        try (Connection c = Database.get()) {
            boolean old = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                ensureProfileForUser(c, userId);
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(old);
            }
        }
    }
}