package com.example.healthflow.service;

import com.example.healthflow.dao.UserDAO;
import com.example.healthflow.dao.UserJdbcDAO;
import com.example.healthflow.db.Database;
import com.example.healthflow.model.Role;
import com.example.healthflow.model.User;
import com.example.healthflow.service.AuthService.Session;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;

/** Service for user authentication and registration */
public class UserAuthService {

    private final UserDAO userDAO = new UserJdbcDAO();

    /** Register a new user (لغير المرضى عادةً) */
    public User register(String fullName, String email, String rawPassword,
                         String nationalId, String phone, String gender, Role role) throws Exception {

        if (rawPassword == null || rawPassword.length() < 5)
            throw new IllegalArgumentException("Weak password");

        String hash = BCrypt.hashpw(rawPassword, BCrypt.gensalt());

        User u = new User();
        u.setNationalId(nationalId);
        u.setFullName(fullName);
        u.setEmail(email);
        u.setPasswordHash(hash);
        u.setRole(role);
        u.setPhone(phone);
        u.setActive(true);
        // ملاحظة: User model عندك قد لا يحوي gender؛ ليس ضروريًا هنا.

        try (Connection c = Database.get()) {
            c.setAutoCommit(false);
            try {
                long userId = userDAO.insertWithGender(
                        nationalId, fullName, email, hash, phone, role.name(), gender, c);
                u.setId(userId);

                // لا تنشئ سجلًا في patients هنا. إن احتجت مريضًا، استعمل PatientService.createPatient

                c.commit();
                Session.set(u);
                return u;
            } catch (Exception ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    /** Login a user */
    public User login(String email, String rawPassword) throws Exception {
        User u = userDAO.findByEmail(email);
        if (u == null) return null;
        boolean ok = BCrypt.checkpw(rawPassword, u.getPasswordHash());
        if (ok) Session.set(u); else Session.clear();
        return ok ? u : null;
    }
}


//package com.example.healthflow.service;
//
//import com.example.healthflow.dao.UserDAO;
//import com.example.healthflow.dao.UserJdbcDAO;
//import com.example.healthflow.db.Database;
//import com.example.healthflow.model.Role;
//import com.example.healthflow.model.User;
//import com.example.healthflow.service.AuthService.Session;
//import org.mindrot.jbcrypt.BCrypt;
//
//import java.sql.Connection;
//
///**
// * Service for user authentication and registration
// */
//public class UserAuthService {
//
//    private final UserDAO userDAO = new UserJdbcDAO();
//
//    /**
//     * Register a new user
//     */
//    public User register(String fullName, String email, String rawPassword,
//                         String nationalId, String phone, Role role) throws Exception {
//
//        if (rawPassword == null || rawPassword.length() < 5)
//            throw new IllegalArgumentException("Weak password");
//
//        String hash = BCrypt.hashpw(rawPassword, BCrypt.gensalt());
//
//        User u = new User();
//        u.setNationalId(nationalId);
//        u.setFullName(fullName);
//        u.setEmail(email);
//        u.setPasswordHash(hash);
//        u.setRole(role);
//        u.setPhone(phone);
//        u.setActive(true);
//
//        try (Connection c = Database.get()) {
//            c.setAutoCommit(false);
//            try {
//                long userId = userDAO.insert(nationalId, fullName, email, hash, phone, role.name(), c);
//                u.setId(userId);
//
//                insertSubtype(c, userId, role);
//
//                c.commit();
//                // Store in session
//                Session.set(u);
//                return u;
//            } catch (Exception ex) {
//                c.rollback();
//                throw ex;
//            } finally {
//                c.setAutoCommit(true);
//            }
//        }
//    }
//
//    /**
//     * Insert user subtype based on role
//     */
//    private void insertSubtype(Connection c, Long userId, Role role) throws Exception {
//        String sql = switch (role) {
//            case DOCTOR -> "INSERT INTO doctors(user_id, specialty) VALUES (?, 'GENERAL')";
//            case PATIENT -> "INSERT INTO patients(user_id) VALUES (?)";
//            case RECEPTIONIST -> "INSERT INTO receptionists(user_id) VALUES (?)";
//            case PHARMACIST -> "INSERT INTO pharmacists(user_id) VALUES (?)";
//            case ADMIN -> null;
//        };
//        if (sql != null) try (var ps = c.prepareStatement(sql)) { ps.setLong(1, userId); ps.executeUpdate(); }
//    }
//
//    /**
//     * Login a user
//     */
//    public User login(String email, String rawPassword) throws Exception {
//        User u = userDAO.findByEmail(email);
//        if (u == null) return null;
//        boolean ok = BCrypt.checkpw(rawPassword, u.getPasswordHash());
//        if (ok) Session.set(u);        // Store in session
//        else Session.clear();
//        return ok ? u : null;
//    }
//}