package com.shareversity.resource;

import com.shareversity.dao.StudentDao;
import com.shareversity.dao.StudentLoginDao;
import com.shareversity.restModels.*;
import com.shareversity.utils.MailClient;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.sql.Timestamp;
import java.util.Date;
import java.util.UUID;

@Path("/shareversity")
public class StudentResource {
    StudentDao studentDao = new StudentDao();
    StudentLoginDao studentLoginDao = new StudentLoginDao();

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello Shareversity Website!";
    }

    @POST
    @Path("/registration")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response sendRegistrationCode(Students students) {

        String studentEmail = students.getEmail();
        String firstName = students.getFirstName();

        // check if the email string is null or empty
        if (studentEmail == null || studentEmail.trim().length() == 0) {
            return Response.status(Response.Status.BAD_REQUEST).
                    entity("Email Id can't be null or empty").build();
        }

        // check if the email id is with edu domain
        // TODO: add method to verify email is valid and exists and also check for .edu

        if(!isValidEmailId(studentEmail)){
            return Response.status(Response.Status.BAD_REQUEST).
                    entity("Please enter a valid student email Id with edu domain").build();
        }

        Students studentObject = studentDao.findUserByEmailId(studentEmail);

        // check if the student is an already registered student
        // if student record exist but not registration is not confirmed then we allow to update
        // new information
        if (studentObject != null && studentObject.getIsCodeVerified()) {
            return Response.status(Response.Status.BAD_REQUEST).
                    entity("Student already exists").build();
        }

        //todo: add security code constraint to be valid only for 15 minutes
        String secretCode = createSecretCode();
        students.setSecretCode(secretCode);

        MailClient.sendRegistrationEmail(firstName, studentEmail, secretCode);

        students.setCreateDate(new Date());

        Students newStudent;
        if (studentObject == null) {
            newStudent = studentDao.addNewStudent(students);
        } else {
            Students existingStudent = studentDao.findUserByEmailId(students.getEmail());
            existingStudent.setPassword(students.getPassword());
            existingStudent.setLastName(students.getLastName());
            existingStudent.setFirstName(students.getFirstName());
            existingStudent.setCreateDate(new Date());
            newStudent = studentDao.updateStudent(existingStudent);
        }

        if (newStudent != null) {
            return Response.status(Response.Status.OK).
                    entity("Security Code is sent Successfully!").build();
        }

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).
                entity("Something Went Wrong" + studentEmail).build();
    }

    @POST
    @Path("/verification")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response verifySecurityCode(EmailVerification emailVerification) {
        String securityCode = emailVerification.getSecurityCode();
        String email = emailVerification.getEmail();

        Students student = studentDao.findUserByEmailId(email);

        if(student==null){
            return Response.status(Response.Status.BAD_REQUEST).
                    entity("Invalid User").build();
        }

        if (student.getIsCodeVerified()) {
            return Response.status(Response.Status.BAD_REQUEST).
                    entity("Security code is already verified").build();
        }

        if (!securityCode.equals(student.getSecretCode())) {
            return Response.status(Response.Status.BAD_REQUEST).
                    entity("Please enter the correct security code").build();
        }

        student.setIsCodeVerified(true);

        //todo: change date to timestamp
        student.setCreateDate(new Date());

        // set code verified to true
        boolean codeVerifiedUpdated = studentDao.updateUserCodeVerified(student);

        if (!codeVerifiedUpdated) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).
                    entity("Something Went Wrong. Please retry.").build();
        }

        StudentLogin studentLogin = new StudentLogin();
        studentLogin.setEmail(student.getEmail());
        studentLogin.setStudentPassword(student.getPassword());

        Date date = new Date();
        Timestamp timeStamp = new Timestamp(date.getTime());
        studentLogin.setLoginTime(timeStamp);

        StudentLogin studentLogin1 = studentLoginDao.createStudentLogin(studentLogin);

        if(studentLogin1==null){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).
                    entity("Something Went Wrong. Please retry.").build();
        }

        return Response.status(Response.Status.OK).
                entity("You have successfully confirmed your account with the " +
                        "email " + email + ". You will use this email address to log in.!").build();
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response loginUser(LoginObject loginInput){
        Students students =
                studentDao.checkIfStudentIsRegistered(loginInput.getUserEmail());

        if(students == null){
            return Response.status(Response.Status.NOT_FOUND).
                    entity("Invalid User " + loginInput.getUserEmail()).build();
        }

        if(!students.getPassword().equals(loginInput.getPassword())){
            return Response.status(Response.Status.NOT_FOUND).
                    entity("Wrong password. Try again or click Forgot password to reset it.").build();
        }

        return Response.status(Response.Status.OK).
                entity("You have successfully logged in!").build();
    }

    @POST
    @Path("/password-change")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response changePassword(PasswordChangeObject passwordChangeObject) {

        // check if the student is a confirmed registered student
        Students student = studentDao.checkIfStudentIsRegistered(passwordChangeObject.getEmail());

        if (student == null) {
            return Response.status(Response.Status.NOT_FOUND).
                    entity("This Email doesn't exist: " + passwordChangeObject.getEmail()).build();
        }

        String enteredOldPassword = passwordChangeObject.getOldPassword();
        String enteredNewPassword = passwordChangeObject.getNewPassword();

        if (enteredOldPassword.equals(enteredNewPassword)) {
            return Response.status(Response.Status.NOT_ACCEPTABLE).
                    entity("The New Password can't be same as Old password.").build();
        }

        if (!student.getPassword().equals(enteredOldPassword)) {
            return Response.status(Response.Status.BAD_REQUEST).
                    entity("Password Incorrect, please enter a correct old password").build();
        }

        student.setPassword(enteredNewPassword);
        studentDao.updateStudent(student);

        return Response.status(Response.Status.OK).
                entity("Password Changed Successfully!").build();
    }

    private boolean isValidEmailId(String userEmail) {
        if(userEmail.contains(".edu")){
            return true;
        }
        return false;
    }

    private String createSecretCode() {
        return UUID.randomUUID().toString().substring(30);
    }
}