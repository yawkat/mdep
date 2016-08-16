/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.mdep.model;

import java.net.URL;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import lombok.Data;

/**
 * @author yawkat
 */
@XmlType
@XmlRootElement
@Data
public class Dependency {
    URL url;
    byte[] sha512sum;
}
