package com.activiti.util.debug;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Performs writing to DEBUG of incoming arguments and outgoing results for a method call.<br>
 * If the method invocation throws an exception, then the incoming arguments are
 * logged to DEBUG as well.<br>
 * The implementation adds very little overhead to a normal method
 * call by only building log messages when required.
 * <p>
 * The logging is done against the logger retrieved using the names:
 * <p>
 * <pre>
 *      targetClassName
 *      targetClassName.methodName
 *      targetClassName.methodName.exception
 * </pre>
 * <p>
 * The following examples show how to control the log levels:
 * <p>
 * <pre>
 *      x.y.MyClass=DEBUG                           # log debug for all method calls
 *      x.y.MyClass.doSomething=DEBUG               # log debug for all doSomething method calls
 *      x.y.MyClass.doSomething.exception=DEBUG     # only log debug for doSomething() upon exception
 * </pre>
 * <p>
 * 
 * @author Derek Hulley
 */
public class MethodCallLogAdvice implements MethodInterceptor
{
    private static int count;
    
    public static int getCount()
    {
        return count;
    }

    public Object invoke(MethodInvocation invocation) throws Throwable
    {
        String methodName = invocation.getMethod().getName();
        String className = invocation.getMethod().getDeclaringClass().getName();
        
        // execute as normal
        try
        {
            Object ret = invocation.proceed();
            // logging
            Log methodLogger = LogFactory.getLog(className + "." + methodName);
            if (methodLogger.isDebugEnabled())  // prevent build string unnecessarily
            {
                // log success
                StringBuffer sb = getInvocationInfo(className, methodName, invocation.getArguments()); 
                sb.append("   Result: ").append(ret);
                methodLogger.debug(sb);
            }
            // done
            return ret;
        }
        catch (Throwable e)
        {
            Log exceptionLogger = LogFactory.getLog(className + "." + methodName + ".exception");
            if (exceptionLogger.isDebugEnabled())
            {
                StringBuffer sb = getInvocationInfo(className, methodName, invocation.getArguments()); 
                sb.append("   Failure: ").append(e.getClass().getName()).append(" - ").append(e.getMessage());
                exceptionLogger.debug(sb);
            }
            // rethrow
            throw e;
        }
    }
    
    /**
     * Return format:
     * <pre>
     *      Method: className#methodName
     *         Argument: arg0
     *         Argument: arg1
     *         ...
     *         Argument: argN {newline}
     * </pre>
     * 
     * @param className
     * @param methodName
     * @param args
     * @return Returns a StringBuffer containing the details of a method call
     */
    private StringBuffer getInvocationInfo(String className, String methodName, Object[] args)
    {
        StringBuffer sb = new StringBuffer(80);
        sb.append("\nMethod: ").append(className).append("#").append(methodName).append("\n");
        for (Object arg : args)
        {
            sb.append("   Argument: ").append(arg).append("\n");
        }
        return sb;
    }
}
