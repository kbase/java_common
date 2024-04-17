package us.kbase.common.service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Helper annotation used to mark methods of generated server side 
 * servlets as RPC-related and to add some properties for their calling.
 * @author rsutormin
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonServerMethod {
	String rpc();
	boolean tuple() default false; // is this used? Workspace returns tuples but it's false
	boolean authOptional() default false;
	boolean async() default true; // removed from JsonServerServlet, not from SDK
	boolean sync() default true;  // removed from JsonServerServlet, not from SDK
}
