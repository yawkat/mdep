/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

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
