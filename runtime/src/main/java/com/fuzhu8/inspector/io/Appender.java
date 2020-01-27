package com.fuzhu8.inspector.io;

/**
 * appender
 * Created by zhkl0228 on 2017/2/6.
 */
public interface Appender {

    void out_print(Object msg);
    void out_println(Object msg);

    void err_print(Object msg);
    void err_println(Object msg);

    void printStackTrace(Throwable throwable);

}
