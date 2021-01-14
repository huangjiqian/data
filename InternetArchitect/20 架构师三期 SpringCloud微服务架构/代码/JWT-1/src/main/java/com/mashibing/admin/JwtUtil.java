package com.mashibing.admin;

import java.util.Base64;
import java.util.Calendar;
import java.util.Date;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;

/**
 * @author yueyi2019
 */
public class JwtUtil {
    /**
     * 密钥，仅服务端存储
     */
    private static String secret = "ko346134h_we]rg3in_yip1!";

    /**
     *
     * @param subject
     * @param issueDate 签发时间
     * @return
     */
    public static String createToken(String subject, Date issueDate) {
    	
    	
        Calendar c = Calendar.getInstance();  
        c.setTime(issueDate);  
        c.add(Calendar.DAY_OF_MONTH, 20);        
        
    	
    	
        String compactJws = Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(issueDate)
                .setExpiration(c.getTime())
                		
                .signWith(io.jsonwebtoken.SignatureAlgorithm.HS512, secret)
                .compact();
        return compactJws;

    }

    /**
     * 解密 jwt
     * @param token
     * @return
     * @throws Exception
     */
    public static String parseToken(String token) {
        try {
            Claims claims = Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
            if (claims != null){
                return claims.getSubject();
            }
        }catch (ExpiredJwtException e){
            e.printStackTrace();
            System.out.println("jwt过期了");
        }

        return "";
    }
    
    
//    public static void main(String[] args) {
//		
//    	String token = createToken("userid=1,role=admin,price=398", new Date());
//	
//    // eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ1c2VyaWQ9MSxyb2xlPWFkbWluLHByaWNlPTM5OCIsImlhdCI6MTU5MDQxMzk3NywiZXhwIjoxNTkyMTQxOTc3fQ.9cduDuvHXdDv4zLQAqvuVDQO9zQbcfkCaWh1-IkWSWwOf2zruX6-hYOeAm6kdhny1BInmtV1Jd8nnsD03OPpug
//
//    	System.out.println(token);
//    
//    }
    
    /*
     *  比如说token设置了30分钟有效，
     *  但是前端如果一直在操作的话按道理应该
     *  是要重新刷新jwt的 这时候并发请求会导
     *  致多个jwt的刷新吧  老师给点建议
     */
    
    
    // hash(base64(头 + 体) + 密文) = 签名  CAS + jwt
   
    
    public static void main(String[] args) {
		byte[] decode = Base64.getDecoder().decode("eyJzdWIiOiJ1c2VyaWQ9MSxyb2xlPWFkbWluLHByaWNlPTM5OCIsImlhdCI6MTU5MDQxMzk3NywiZXhwIjoxNTkyMTQxOTc3fQ");
		System.out.println(new String(decode));
		
		String parseToken = parseToken("eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ1c2VyaWQ9MSxyb2xlPWFkbWluLHByaWNlPTM5OCIsImlhdCI6MTU5MDQxMzk3NywiZXhwIjoxNTkyMTQxOTc3fQ.9cduDuvHXdDv4zLQAqvuVDQO9zQbcfkCaWh1-IkWSWwOf2zruX6-hYOeAm6kdhny1BInmtV1Jd8nnsD03OP2ug");
		
		System.out.println("parseToken:" + parseToken);
	}

}