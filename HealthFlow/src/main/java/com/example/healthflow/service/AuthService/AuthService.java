package com.example.healthflow.service.AuthService;
import com.example.healthflow.dao.UserDAO;
import com.example.healthflow.db.Database;
import com.example.healthflow.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class AuthService {
    private final UserDAO userDAO = new UserDAO();

    public User register(String fullName, String email, String rawPassword,
                         String role, String nationalId) throws Exception {

        if (rawPassword == null || rawPassword.length() < 5)
            throw new IllegalArgumentException("Weak password");

        String hash = BCrypt.hashpw(rawPassword, BCrypt.gensalt());
        User u = new User(null, nationalId, fullName, email, hash, role);

        // Insert user + subtype in one transaction
        try (Connection c = Database.get()) {
            c.setAutoCommit(false);
            try {
                u = userDAO.insert(u);
                insertSubtype(c, u.getId(), role);
                c.commit();
                return u;
            } catch (Exception ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    private void insertSubtype(Connection c, Long userId, String role) throws Exception {
        String sql = switch (role) {
            case "DOCTOR"       -> "INSERT INTO doctors(user_id, specialty) VALUES (?, 'GENERAL')";
            case "PATIENT"      -> "INSERT INTO patients(user_id, date_of_birth, gender) VALUES (?, CURRENT_DATE, 'OTHER')";
            case "RECEPTIONIST" -> "INSERT INTO receptionists(user_id) VALUES (?)";
            case "PHARMACIST"   -> "INSERT INTO pharmacists(user_id) VALUES (?)";
            case "ADMIN"        -> null;
            default -> throw new IllegalArgumentException("Unknown role: " + role);
        };
        if (sql != null) try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        }
    }

    public User login(String email, String rawPassword) throws Exception {
        User u = userDAO.findByEmail(email);
        if (u == null) return null;
        return BCrypt.checkpw(rawPassword, u.getPasswordHash()) ? u : null;
    }
}