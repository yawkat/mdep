package at.yawk.mdep;

import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@RequiredArgsConstructor
class JulLogger implements Logger {
    private final java.util.logging.Logger julLogger;

    @Override
    public void info(String msg) {
        julLogger.info(msg);
    }

    @Override
    public void warn(String msg) {
        julLogger.warning(msg);
    }
}
