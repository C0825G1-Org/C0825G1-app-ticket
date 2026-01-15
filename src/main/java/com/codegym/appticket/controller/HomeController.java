package com.codegym.appticket.controller;

import com.codegym.appticket.repository.IEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class HomeController {
    @Autowired
    private IEventRepository eventRepository;

    @GetMapping
    public String showHomePage(){
        return "home/index";
    }

}
