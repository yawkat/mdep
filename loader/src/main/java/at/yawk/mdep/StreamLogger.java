package at.yawk.mdep;

import java.io.PrintStream;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@RequiredArgsConstructor
class StreamLogger implements Logger {
    private final PrintStream stream;

    @Override
    public void info(String msg) {
        stream.println("[INFO] " + msg);
    }

    @Override
    public void warn(String msg) {
        stream.println("[WARN] " + msg);
    }
}
