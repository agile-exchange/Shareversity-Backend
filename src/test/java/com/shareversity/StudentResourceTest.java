package com.shareversity;

import com.shareversity.dao.StudentDao;
import com.shareversity.resource.StudentResource;
import com.shareversity.restModels.*;
import jakarta.ws.rs.core.Response;
import org.junit.*;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Date;

public class StudentResourceTest {

    private static StudentResource studentResource;
    private static StudentDao studentDao;
    public StudentResourceTest(){

    }
    @BeforeClass
    public static void init() {
        studentResource = new StudentResource();
        studentDao = new StudentDao();
        setupAddRecords();
    }

    public static void setupAddRecords() {
        Students newStudent1 = new Students(1,"Test1","Student1",
                "teststudent1@g.university.edu","testpassword1",new Date(),"12345",true);
        Students newStudent2 = new Students(2,"Test2","Student2",
                "teststudent2@g.university.edu","testpassword2",new Date(),"123456",true);
        Students newStudent3 = new Students(3,"Test3","Student3",
                "teststudent3@g.university.edu","testpassword3",new Date(),"123457",true);
        Students newStudent7 = new Students(4,"Test7","Student7",
                "teststudent7@g.university.edu","testpassword7",new Date(),"123457",true);
        Students newStudent9 = new Students(5,"Test9","Student9",
                "teststudent9@g.university.edu","testpassword9",new Date(),"123457",false);

        studentDao.addNewStudent(newStudent1);
        studentDao.addNewStudent(newStudent2);
        studentDao.addNewStudent(newStudent3);
        studentDao.addNewStudent(newStudent7);
        studentDao.addNewStudent(newStudent9);
    }
    @Test
    public void testSendRegistrationEmailInvalidEmail(){
        Students students = new Students();
        students.setEmail("tomcat@gmail.com");
        students.setFirstName("Tom");
        students.setLastName("Cat");
        students.setPassword("123456");
        students.setCreateDate(new Date());
        students.setSecretCode("xyzabc");
        Response response = studentResource.sendRegistrationCode(students);
        String result = (String) response.getEntity();
        Assert.assertEquals("Please enter a valid student email Id with edu domain" , result);
    }

    @Test
    public void testSendRegistrationEmailNullEmailId(){
        Students students = new Students();
        students.setEmail(null);
        Response response = studentResource.sendRegistrationCode(students);
        String result = (String) response.getEntity();
        Assert.assertEquals("Email Id can't be null or empty" , result);
    }

    @Test
    public void testSendRegistrationEmailEmptyEmailId(){
        Students students = new Students();
        students.setEmail("");
        Response response = studentResource.sendRegistrationCode(students);
        String result = (String) response.getEntity();
        Assert.assertEquals("Email Id can't be null or empty" , result);
    }
    @Test
    public void testLoginInvalidUser(){
        LoginObject loginObject = new LoginObject();
        loginObject.setUserEmail("tomcat@husky.edu");
        loginObject.setPassword("123");
        Response response = studentResource.loginUser(loginObject);
        String result = (String) response.getEntity();
        Assert.assertEquals("Invalid User tomcat@husky.edu" , result);
    }

    @Test
    public void testVerificationCodeInvalidUser(){
        EmailVerification emailVerification = new EmailVerification();
        emailVerification.setEmail("xyz@husky.edu");
        emailVerification.setSecurityCode("xyz");
        Response response = studentResource.verifySecurityCode(emailVerification);
        String result = (String) response.getEntity();
        Assert.assertEquals("Invalid User" , result);
    }

    @Test
    public void sendRegistrationEmailValidEmail(){
        Students students = new Students();
        students.setFirstName("Tomcat");
        students.setLastName("Server");
        students.setEmail("tomcat1@husky.edu");
        students.setPassword("password");
        Response response = studentResource.sendRegistrationCode(students);
        String result = (String) response.getEntity();
        Assert.assertEquals("Security Code is sent Successfully!" , result);
    }


    @Test
    public void testVerificationCodeValidUser(){
        Students students = new Students();
        students.setEmail("test5@husky.edu");
        students.setFirstName("Test5");
        students.setLastName("Student5");
        students.setPassword("password5");
        students.setCreateDate(new Date());
        studentResource.sendRegistrationCode(students);

        StudentDao studentDao = new StudentDao();
        Students userByEmailId = studentDao.findUserByEmailId(students.getEmail());
        EmailVerification emailVerification = new EmailVerification();
        emailVerification.setSecurityCode(userByEmailId.getSecretCode());
        emailVerification.setEmail(userByEmailId.getEmail());
        Response response = studentResource.verifySecurityCode(emailVerification);
        String result = (String) response.getEntity();
        Assert.assertEquals("You have successfully confirmed your account with the " +
                "email " + emailVerification.getEmail() + ". You will use this email address to log in.!" , result);
    }

    @Test
    public void testVerificationCodeValidUserButInvalidCode(){
        Students students = new Students();
        students.setFirstName("Tomcat");
        students.setLastName("Server");
        students.setEmail("tomcat5@husky.edu");
        students.setPassword("password");
        studentResource.sendRegistrationCode(students);

        StudentDao studentDao = new StudentDao();
        Students userByEmailId = studentDao.findUserByEmailId(students.getEmail());
        EmailVerification emailVerification = new EmailVerification();
        emailVerification.setSecurityCode("xyz");
        emailVerification.setEmail(userByEmailId.getEmail());
        Response response = studentResource.verifySecurityCode(emailVerification);
        String result = (String) response.getEntity();
        Assert.assertEquals("Please enter the correct security code" , result);
    }

    @Test
    public void testChangePasswordForInvalidUser(){
        PasswordChangeObject passwordChangeObject = new PasswordChangeObject
                ("testInvalidUser@edu.com","oldPassword","newPassword");
        Response response = studentResource.changePassword(passwordChangeObject);
        String result = (String) response.getEntity();
        Assert.assertEquals("This Email doesn't exist: " + passwordChangeObject.getEmail(), result);
    }

    @Test
    public void testChangePasswordForIncorrect(){
        PasswordChangeObject passwordChangeObject = new PasswordChangeObject
                ("teststudent2@g.university.edu","testpassword3","newPassword");
        Response response = studentResource.changePassword(passwordChangeObject);
        String result = (String) response.getEntity();
        Assert.assertEquals("Password Incorrect, please enter a correct old password", result);
    }

    @Test
    public void testChangePasswordForOldAndNewSamePassword(){
        PasswordChangeObject passwordChangeObject = new PasswordChangeObject
                ("teststudent2@g.university.edu","testpassword2","testpassword2");
        Response response = studentResource.changePassword(passwordChangeObject);
        String result = (String) response.getEntity();
        Assert.assertEquals("The New Password can't be same as Old password.", result);
    }

    @Test
    public void testChangePasswordForSuccess(){
        PasswordChangeObject passwordChangeObject = new PasswordChangeObject
                ("teststudent2@g.university.edu","testpassword2","testpassword5");
        Response response = studentResource.changePassword(passwordChangeObject);
        String result = (String) response.getEntity();
        Assert.assertEquals("Password Changed Successfully!", result);
    }

    @Test
    public void testPasswordResetForNullInput() {
        PasswordResetObject passwordResetObject = new PasswordResetObject(null);
        Response response = studentResource.sendPasswordResetLink(passwordResetObject);
        Assert.assertEquals("Email Id can't be null", response.getEntity());
    }
    @Test
    public void testPasswordResetForInvalidEmailId(){
        PasswordResetObject passwordResetObject =
                new PasswordResetObject("teststudent6@harvard.edu");
        Response response = studentResource.sendPasswordResetLink(passwordResetObject);
        String result = (String) response.getEntity();
        Assert.assertEquals("Email doesn't exist, please enter a valid email for password reset.", result);
    }
    @Test
    public void testPasswordResetForValidEmailId(){
        PasswordResetObject passwordResetObject =
                new PasswordResetObject("teststudent7@g.university.edu");
        Response response = studentResource.sendPasswordResetLink(passwordResetObject);
        String result = (String) response.getEntity();
        Assert.assertEquals("Password Reset link sent successfully!", result);
    }
    @Test
    public void testPasswordResetForEmptyEmailString(){
        PasswordResetObject passwordResetObject = new PasswordResetObject("");
        Response response = studentResource.sendPasswordResetLink(passwordResetObject);
        Assert.assertEquals("Email can't be an Empty String, please enter a valid email", (String) response.getEntity());
    }
    @Test
    public void testPasswordResetForUnregisteredStudent(){
        PasswordResetObject passwordResetObject = new PasswordResetObject("teststudent9@g.university.edu");
        Response response = studentResource.sendPasswordResetLink(passwordResetObject);
        Assert.assertEquals("Password can't be reset, please register first.", (String) response.getEntity());
    }


    @AfterClass
    public static void deleteForDuplicateDatabase() {
        StudentDao studentDao = new StudentDao();
        studentDao.deleteStudent("test5@husky.edu");
        studentDao.deleteStudent("tomcat5@husky.edu");
        studentDao.deleteStudent("tomcat1@husky.edu");
        studentDao.deleteStudent("teststudent1@g.university.edu");
        studentDao.deleteStudent("teststudent2@g.university.edu");
        studentDao.deleteStudent("teststudent3@g.university.edu");
        studentDao.deleteStudent("teststudent7@g.university.edu");
        studentDao.deleteStudent("teststudent9@g.university.edu");
    }
}
