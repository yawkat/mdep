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
