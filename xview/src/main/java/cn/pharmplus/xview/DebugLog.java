/***
This is free and unencumbered software released into the public domain.

Anyone is free to copy, modify, publish, use, compile, sell, or
distribute this software, either in source code form or as a compiled
binary, for any purpose, commercial or non-commercial, and by any
means.

For more information, please refer to <http://unlicense.org/>
*/

package cn.pharmplus.xview;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;


/**
 * @date 21.06.2012
 * @author Mustafa Ferhan Akman
 * 
 * Create a simple and more understandable Android logs. 
 * */

public class DebugLog {

	static String className;
	static String methodName;
	static int lineNumber;
	
    private DebugLog(){
        /* Protect from instantiations */
    }

	public static boolean isDebuggable() {
		return true;//BuildConfig.DEBUG;
	}

	private static String createLog( String log ) {

		StringBuffer buffer = new StringBuffer();
		buffer.append("[");
		buffer.append(methodName);
		buffer.append(":");
		buffer.append(lineNumber);
		buffer.append("]");
		buffer.append(log);

		return buffer.toString();
	}
	
	private static void getMethodNames(StackTraceElement[] sElements){
		className = sElements[1].getFileName();
		methodName = sElements[1].getMethodName();
		lineNumber = sElements[1].getLineNumber();
	}

	public static void e(String message){
		// Throwable instance must be created before any methods  
		getMethodNames(new Throwable().getStackTrace());
		e(className, message);
	}
	
	public static void e(String tag,String message){
		if (!isDebuggable())
			return;
		Log.e(tag, createLog(message));
	}

	public static void i(String message){
		getMethodNames(new Throwable().getStackTrace());
		i(className, message);
	}
	
	public static void i(String tag,String message){
		if (!isDebuggable())
			return;
		Log.i(tag, createLog(message));
	}
	
	public static void d(String message){
		getMethodNames(new Throwable().getStackTrace());
		d(className, message);
	}
	
	public static void d(String tag,String message){
		if (!isDebuggable())
			return;
		Log.d(tag, createLog(message));
	}
	
	public static void v(String message){
		getMethodNames(new Throwable().getStackTrace());
		v(className, message);
	}
	
	public static void v(String tag,String message){
		if (!isDebuggable())
			return;
		Log.v(tag, createLog(message));
	}
	
	public static void w(String message){
		getMethodNames(new Throwable().getStackTrace());
		w(className, message);
	}
	
	public static void w(String tag,String message){
		if (!isDebuggable())
			return;
		Log.w(tag, createLog(message));
	}
	
	@TargetApi(Build.VERSION_CODES.FROYO)
	public static void wtf(String message){
		getMethodNames(new Throwable().getStackTrace());
		wtf(className, message);
	}
	
	@TargetApi(Build.VERSION_CODES.FROYO)
	public static void wtf(String tag,String message){
		if (!isDebuggable())
			return;
		Log.wtf(tag, createLog(message));
	}

}
