/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

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
