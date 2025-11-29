package com.mediclinic.dao;

import com.mediclinic.model.User;
import com.mediclinic.util.HibernateUtil;
import org.hibernate.Session;

public class UserDAO extends AbstractDAO<User, Long> {

    public UserDAO() {
        super();
    }

    public User findByUsername(String username) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", username)
                    .uniqueResult();
        }
    }
}