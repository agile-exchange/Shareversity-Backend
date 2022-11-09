package com.shareversity.dao;

import com.shareversity.restModels.StudentLogin;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.List;

public class StudentLoginDao {
    private SessionFactory factory;

    /**
     * Default Constructor.
     */
    public StudentLoginDao() {
        // it will check the hibernate.cfg.xml file and load it
        // next it goes to all table files in the hibernate file and loads them
        this.factory = StudensSessionFactory.getFactory();
    }

    /**
     * Find a student login based on their email.
     *
     * @param email Student's email.
     * @return the Student login object if found; null otherwise.
     */
    public StudentLogin findStudentLoginsByEmail(String email) {
        Session session = factory.openSession();
        try {
            org.hibernate.query.Query query = session.createQuery("FROM StudentLogin WHERE email = :email ");
            query.setParameter("email", email);
            List list = query.list();
            if (list.isEmpty()) {
                return null;
            }
            return (StudentLogin) list.get(0);
        } finally {
            if (session != null) { session.close(); }
        }
    }

    /**
     * Create a student login. This will only be successful if a student
     * is a registered student
     *
     * @param studentLogin object that wants to be created.
     * @return newly created Student Login if sucessfull, throw a hibernate exception
     * otherwise.
     */
    public synchronized StudentLogin createStudentLogin(StudentLogin studentLogin) {
        Session session = factory.openSession();
        Transaction tx = null;
        if (findStudentLoginsByEmail(studentLogin.getEmail()) != null) {
            throw new HibernateException("Student Login already exists.");
        }
        try {
            tx = session.beginTransaction();
            session.save(studentLogin);
            tx.commit();
        } catch (HibernateException e) {
            if (tx != null) tx.rollback();
            throw new HibernateException(e);
        } finally {
            session.close();
        }

        return studentLogin;
    }

    /**
     * Update a student login object
     *
     * @param studentLogin updated student login object.
     * @return true if updated; hibernate exception otherwise.
     */
    public synchronized boolean updateStudentLogin(StudentLogin studentLogin) {
        Session session = factory.openSession();
        Transaction tx = null;
        if (findStudentLoginsByEmail(studentLogin.getEmail()) != null) {
            try {
                tx = session.beginTransaction();
                session.saveOrUpdate(studentLogin);
                tx.commit();
            } catch (HibernateException e) {
                if (tx != null) tx.rollback();
                throw new HibernateException(e);
            } finally {
                session.close();
            }
        } else {
            throw new HibernateException("Student Login with email: " + studentLogin.getEmail() +
                    " not found.");
        }
        return true;
    }

    /**
     * Delete a student login based on email.
     *
     * @param email corresponding email to the student login object that wants
     *              to be deleted.
     * @return true if deleted; hibernate exception otherwise.
     */
    public synchronized boolean deleteStudentLogin(String email) {
        Session session = factory.openSession();
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email argument cannot be null or empty.");
        }

        StudentLogin studentLogin = findStudentLoginsByEmail(email);
        if (studentLogin != null) {
            Transaction tx = null;
            try {
                tx = session.beginTransaction();
                session.delete(studentLogin);
                tx.commit();
            } catch (HibernateException e) {
                if (tx != null) tx.rollback();
                throw new HibernateException(e);
            } finally {
                session.close();
            }
        } else {
            throw new HibernateException("Student Login with email: " + email +
                    " not found.");
        }
        return true;
    }
}