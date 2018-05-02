TestTime consists of a maven plugin and extension that show the top N slowest unit tests and suites.
# Single Module Projects

For single module builds, use the plugin. It will display the 5 slowest suites and tests in the log 
after the test phase, and it will create a `testtimes.txt` file in the `target` directory 
with the 20 slowest.

```
  <build>
    <plugins>      
      <plugin>
        <groupId>ca.nanometrics.maven.plugins</groupId>
        <artifactId>maven-testtime-plugin</artifactId>
        <version>1.0.0</version>
        <executions>
          <execution>
            <goals>
              <goal>display</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
    </plugins>      
  </build>
```

# Multi-Module Projects

For multi-module builds, you can use the extension to get a single `testtimes.txt` file in the top level 
project `target` directory, if you want.

In the top-level pom, put:

```
  <build>      
    <extensions>
      <extension>
        <groupId>ca.nanometrics.maven.plugins</groupId>
        <artifactId>testtime-maven-extension</artifactId>
        <version>1.0.0</version>
      </extension>
    </extensions>
  </build>
```

You can still use the plugin above on multi-module projects too, and get individual project reports, if desired.

# Properties

There are 2 system properties to control the number of results that are shown.

* testtime.loglimit  - the number of slow tests/suites to show in the log
* testtime.filelimit - the number of slow tests/suites to show in the file

e.g. `mvn -Dtesttime.loglimit=10 install`


To use the plugin, add the following to the POM:

      
      