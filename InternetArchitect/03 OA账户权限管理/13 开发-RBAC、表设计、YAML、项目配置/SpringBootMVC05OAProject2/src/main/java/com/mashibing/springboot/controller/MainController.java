package com.mashibing.springboot.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
public class MainController {
	
	@RequestMapping("/")
	public String index () {
		return "index";
	}
	
	@RequestMapping("index")
	public String index1 () {
		return "/index";
	}
}
