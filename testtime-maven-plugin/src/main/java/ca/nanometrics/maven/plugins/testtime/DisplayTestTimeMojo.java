package ca.nanometrics.maven.plugins.testtime;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Output list of slowest test suites and test methods.
 */
@Mojo(name = "display", defaultPhase = LifecyclePhase.VERIFY)
public class DisplayTestTimeMojo extends AbstractMojo
{
  /**
   * The number of test times to show in the maven log output.
   */
  @Parameter(alias = "logLimit", property = TestTimes.LOG_LIMIT_PROPERTY, defaultValue = "5")
  private int m_logLimit;

  /**
   * The number of test times to show in the output file.
   */
  @Parameter(alias = "fileLimit", property = TestTimes.FILE_LIMIT_PROPERTY, defaultValue = "0")
  private int m_fileLimit;

  @Parameter(alias = "directories", property = TestTimes.SEARCH_DIRECTORIES_PROPERTY,
      defaultValue = "surefire-reports,failsafe-reports")
  private List<String> m_directoriesToSearch;

  @Parameter(property = "project.basedir")
  private File m_projectBasedir;

  @Parameter(property = "project.build.directory")
  private File m_buildDirectory;

  @Parameter(property = "project.packaging")
  private String m_packaging;

  @Parameter(property = "skipTests", defaultValue = "false")
  private boolean m_skipTests;

  @Parameter(defaultValue = "${session}")
  private MavenSession m_mavenSession;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException
  {
    if (m_skipTests)
    {
      getLog().info("Skipping test times because tests are skipped.");
      return;
    }
    if (m_packaging.equals("pom"))
    {
      getLog().info("Skipping test times for packaging POM.");
      return;
    }
    TestTimes testTimes =
        new TestTimes((msg, e) -> getLog().info(msg, e), m_logLimit, m_fileLimit, m_directoriesToSearch);
    testTimes.processBuildDirectories(Paths.get(m_buildDirectory.getAbsolutePath()),
        Collections.singleton(m_buildDirectory.getAbsolutePath()));
    getLog().info(
        "Slowest test times written to " + m_buildDirectory.getAbsolutePath() + File.separator + TestTimes.FILE_NAME);
  }
}
