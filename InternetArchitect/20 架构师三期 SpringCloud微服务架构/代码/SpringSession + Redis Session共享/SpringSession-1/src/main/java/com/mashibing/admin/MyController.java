package com.mashibing.admin;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MyController {

	@GetMapping
	public String list(HttpServletRequest req) {
		req.getSession().setAttribute("aaa", "ooo");
		return "xxoo";
	}
	
	
	@GetMapping("/get")
	public String get(HttpServletRequest req) {
		Object attribute = req.getSession().getAttribute("aaa");
		System.out.println(attribute.toString());
		return "xxoo";
	}
}
