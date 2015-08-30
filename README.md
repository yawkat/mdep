mdep
====

Java dependency loader

General Setup
-------------

**Maven:**
```
<project>
    [...]

    <dependencies>
        [...]
        
        <dependency>
            <groupId>at.yawk.mdep</groupId>
            <artifactId>mdep-loader</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>at.yawk.mdep</groupId>
                <artifactId>mdep-maven-plugin</artifactId>
                <version>1.0-SNAPSHOT</version>
                
                <executions>
                    <execution>
                        <phase>generate-resources</phase>
                        <goals>
                            <goal>generate</goal>                            
                        </goals>
                    </execution>
                </executions>

                <configuration>
                    <excludes>
                        <!-- Add artifacts you do not want to include - that's probably this project. -->
                        [...]
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

**Java:**
```
new DependencyLoader()
        .downloadDependencies()
        .toParentLastClassLoader()
        .loadClass("MyClass");
```

Bukkit
------

```
public class MyLoader extends JavaPlugin {
    @Override
    public void onLoad() {
        try {
            new DependencyLoader(getLogger())
                    .downloadDependencies()
                    .addUrls(((URLClassLoader) getClassLoader()).getURLs())
                    .toParentLastClassLoader()
                    .loadClass("MyPlugin").newInstance();
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Failed to load cricket dependencies, cricket will not start!", e);
            return;
        }
    }
}