package com.shareversity;

import com.shareversity.dao.StudentDao;
import com.shareversity.resource.StudentResource;
import com.shareversity.restModels.LoginObject;
import com.shareversity.restModels.Students;
import jakarta.ws.rs.core.Response;
import org.junit.*;

import java.util.Date;

public class StudentLoginTest {

    private static StudentResource studentResource;
    private static StudentDao studentDao;

    @BeforeClass
    public static void init() {
        studentResource = new StudentResource();
        studentDao = new StudentDao();
        setupAddRecords();
    }

    public static void setupAddRecords(){
        Students newStudent1 = new Students(1,"Test1","Student1",
                "teststudent1@g.university.edu","testpassword1",new Date(),"12345",true);

        studentDao.addNewStudent(newStudent1);
    }

    @Test
    public void testLoginForRegisteredStudent(){
        LoginObject loginObject = new LoginObject();
        loginObject.setUserEmail("teststudent1@g.university.edu");
        loginObject.setPassword("testpassword1");
        Response response = studentResource.loginUser(loginObject);
        String result = (String) response.getEntity();
        Assert.assertEquals("You have successfully logged in!",result);
    }

    @Test
    public void testLoginForInvalidStudent(){
        LoginObject loginObject = new LoginObject();
        loginObject.setUserEmail("teststudent1Inavlid@g.university.edu");
        loginObject.setPassword("testpasswordinvalid1");
        Response response = studentResource.loginUser(loginObject);
        String result = (String) response.getEntity();
        Assert.assertEquals("Invalid User " + loginObject.getUserEmail(),result);
    }

    @Test
    public void testLoginForIncorrectPassword(){
        LoginObject loginObject = new LoginObject();
        loginObject.setUserEmail("teststudent1@g.university.edu");
        loginObject.setPassword("testpasswordinvalid1");
        Response response = studentResource.loginUser(loginObject);
        String result = (String) response.getEntity();
        Assert.assertEquals("Wrong password. Try again or click Forgot password to reset it.", result);
    }

    @AfterClass
    public static void deleteForDuplicateDatabase() {
        studentDao.deleteStudent("teststudent1@g.university.edu");
    }
}
