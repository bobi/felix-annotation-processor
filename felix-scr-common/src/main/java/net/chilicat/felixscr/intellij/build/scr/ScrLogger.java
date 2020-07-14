package net.chilicat.felixscr.intellij.build.scr;

public interface ScrLogger {

    boolean isErrorPrinted();

    boolean isDebugEnabled();

    void debug(String content);

    void debug(String content, Throwable error);

    void debug(Throwable error);

    boolean isInfoEnabled();

    void info(String content);

    void info(String content, Throwable error);

    void info(Throwable error);

    boolean isWarnEnabled();

    void warn(String content);

    void warn(String content, String location, int lineNumber);

    void warn(String content, String location, int lineNumber, int columNumber);

    void warn(String content, Throwable error);

    void warn(Throwable error);

    boolean isErrorEnabled();

    void error(String content);

    void error(String content, String location, int lineNumber);

    void error(String content, String location, int lineNumber, int columNumber);

    void error(String content, Throwable error);

    void error(Throwable error);
}
