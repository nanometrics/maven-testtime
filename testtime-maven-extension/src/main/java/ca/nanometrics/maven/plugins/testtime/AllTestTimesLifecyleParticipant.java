package ca.nanometrics.maven.plugins.testtime;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

@Component(role = AbstractMavenLifecycleParticipant.class)
public class AllTestTimesLifecyleParticipant extends AbstractMavenLifecycleParticipant
{
  private static final int DEFAULT_LOG_LIMIT = 0;
  private static final int DEFAULT_FILE_LIMIT = 20;

  @Requirement
  Logger logger;

  @Override
  public void afterSessionEnd(MavenSession mavenSession) throws MavenExecutionException
  {
    List<String> goals = mavenSession.getGoals();
    if (isAllDirectGoals(goals))
    {
      boolean surefire = goals.stream().anyMatch(goal -> goal.contains("surefire") || goal.contains("failsafe"));
      if (!surefire)
      {
        logger.debug("Skipping test times because no surefire or failsafe goals run.");
        return;
      }
    }

    String property = mavenSession.getSystemProperties().getProperty("skipTests");
    boolean skipTests = property != null && (property.trim().isEmpty() || Boolean.parseBoolean(property));
    if (skipTests)
    {
      logger.info("Skipping test times because tests are skipped.");
      return;
    }
    int logLimit = getLimit(mavenSession, TestTimes.LOG_LIMIT_PROPERTY, DEFAULT_LOG_LIMIT);
    int fileLimit = getLimit(mavenSession, TestTimes.FILE_LIMIT_PROPERTY, DEFAULT_FILE_LIMIT);
    List<String> directoriesToSearch = getSearchDirectories(mavenSession);
    TestTimes testTimes = new TestTimes((msg, e) -> logger.info(msg, e), logLimit, fileLimit, directoriesToSearch);
    List<String> directories = mavenSession.getAllProjects().stream().map(project -> project.getBuild().getDirectory())
        .collect(Collectors.toList());
    String outputFolder = mavenSession.getTopLevelProject().getBuild().getDirectory();
    testTimes.processBuildDirectories(Paths.get(outputFolder), directories);
    logger.info("Slowest test times for all modules written to " + new File(outputFolder, TestTimes.FILE_NAME));
  }

  /**
   * Maven can be run with phases or direct goals. Direct goals always have a colon in them of the form
   * <code>plugin:mojo</code> (like sortpom:sort), or the full-form <code>groupId:artifactId:version:goal</code> (where
   * version is optional). Conversely, phases do not have a colon (eg <code>mvn install</code>).<br>
   * We could go further an ensure that surefire/failsafe has been run in the lifecycle at all, but that is perhaps one
   * step too far to check here.
   *
   * @return true if all the given maven commands are of the form <code>plugin:goal</code>, false otherwise.
   */
  private boolean isAllDirectGoals(List<String> goals)
  {
    return goals.stream().allMatch(goal -> goal.contains(":"));
  }

  private int getLimit(MavenSession mavenSession, String property, int defaultLimit)
  {
    String resultsLimit = mavenSession.getSystemProperties().getProperty(property);
    if (resultsLimit != null)
    {
      try
      {
        return Integer.parseInt(resultsLimit);
      }
      catch (NumberFormatException e)
      {
        logger.warn("Could not parse limit " + property + "=" + resultsLimit + ". Using default " + defaultLimit + ".");
      }
    }
    return defaultLimit;
  }

  private List<String> getSearchDirectories(MavenSession mavenSession)
  {
    String property = mavenSession.getSystemProperties().getProperty(TestTimes.SEARCH_DIRECTORIES_PROPERTY);
    if (property == null)
    {
      return TestTimes.DEFAULT_SEARCH_DIRECTORIES;
    }
    return Arrays.asList(property.split(","));
  }
}
