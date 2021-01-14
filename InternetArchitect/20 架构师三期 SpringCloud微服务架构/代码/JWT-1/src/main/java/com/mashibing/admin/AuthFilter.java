package com.mashibing.admin;

import java.io.IOException;
import java.util.logging.LogRecord;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@WebFilter(urlPatterns = "/**")
@Component
public class AuthFilter implements Filter{


	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// TODO Auto-generated method stub
		//Filter.super.init(filterConfig);
		System.out.println("auth 启动成功...");
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		
		HttpServletRequest req =(HttpServletRequest)request;
		
		String token = req.getHeader("token");
		
		
		if(StringUtils.isEmpty(token)) {
			
			System.err.println("你没登录");
		}else {
			// 续期 自动续 - 时间间隔，手动虚
			/**
			 * 来 明哥快给我解释一下 这种无状态的登录验证怎么做重复登录踢出之前的
	                                 这需求很普遍啊 只能短有效时间 然后记录token下发和userId的关系 每次续期的时候验证？
	                                 
	           下发了两个token      上下文                
			 */
			String parseToken = JwtUtil.parseToken(token);
			
			
			if(!StringUtils.isEmpty(parseToken)) {
				
				System.out.println("里面请...");
				chain.doFilter(request, response);
			}
			
		}
		
		System.out.println("xxoo 来了老弟~");
	}

}
