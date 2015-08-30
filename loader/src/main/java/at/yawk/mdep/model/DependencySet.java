package at.yawk.mdep.model;

import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import lombok.Data;

/**
 * @author yawkat
 */
@Data
@XmlType
@XmlRootElement
public class DependencySet {
    List<Dependency> dependencies;
}
