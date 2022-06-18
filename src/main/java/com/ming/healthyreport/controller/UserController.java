package com.ming.healthyreport.controller;

import com.ming.healthyreport.model.User;
import com.ming.healthyreport.repository.UserRepository;
import com.ming.healthyreport.task.DailyReport;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping("/user")
public class UserController {
    private final UserRepository repository;
    private final DailyReport dailyReport;

    public UserController(UserRepository repository, DailyReport dailyReport) {
        this.repository = repository;
        this.dailyReport = dailyReport;
    }

    @RequestMapping(value = "{id}", method = RequestMethod.GET)
    public ResponseEntity<User> getUserByID(@PathVariable("id") User user) {
        return new ResponseEntity<User>(user, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<Object> addUser(@RequestBody User user) {
        repository.save(user);
        return new ResponseEntity<Object>(HttpStatus.OK);
    }

    @RequestMapping(value = "{id}", method = RequestMethod.DELETE)
    public ResponseEntity<Boolean> deleteUser(@PathVariable("id") Long id) {
        repository.deleteById(id);
        return new ResponseEntity<Boolean>(HttpStatus.OK);
    }


    @RequestMapping("/begin")
    public String begin() {
        dailyReport.scheduledReport();
        return "error";
    }
}
