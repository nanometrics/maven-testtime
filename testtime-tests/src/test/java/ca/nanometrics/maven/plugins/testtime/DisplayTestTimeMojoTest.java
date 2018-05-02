package ca.nanometrics.maven.plugins.testtime;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.io.FileMatchers.anExistingDirectory;
import static org.hamcrest.io.FileMatchers.anExistingFile;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.junit.Test;

public class DisplayTestTimeMojoTest
{
  private static final String GROUPID = "ca.nanometics.foobar";
  private static final String VERSION = "1.0";
  private static final String FOOBAR = "foobar";
  private static final String BAR = "bar";
  private static final String FOO = "foo";
  private static final String JAR = "jar";
  private static final String POM = "pom";

  @Test
  public void testBasics() throws Exception
  {
    File projectDir = ResourceExtractor.simpleExtractResources(getClass(), "/multi-module-project");
    assertThat(projectDir, is(notNullValue()));
    assertThat(projectDir.exists(), is(true));

    Verifier verifier = new Verifier(projectDir.getAbsolutePath());

    verifier.deleteArtifact(GROUPID, FOOBAR, VERSION, POM);
    verifier.deleteArtifact(GROUPID, FOOBAR, VERSION, JAR);
    verifier.deleteArtifact(GROUPID, FOO, VERSION, POM);
    verifier.deleteArtifact(GROUPID, FOO, VERSION, JAR);
    verifier.deleteArtifact(GROUPID, BAR, VERSION, POM);
    verifier.deleteArtifact(GROUPID, BAR, VERSION, JAR);

    // verifier.setAutoclean(false);
    List<String> cliOptions = new ArrayList<>();
    cliOptions.add("-Dmaven.repo.local=" + new File(projectDir, "localrepo").getAbsolutePath());
    verifier.setCliOptions(cliOptions);
    verifier.executeGoal("install");

    verifier.verifyErrorFreeLog();
    verifier.verifyTextInLog("Slowest test times for all modules written to");

    File outputDir = new File(verifier.getBasedir(), "target");
    assertThat(outputDir.toString(), outputDir, is(anExistingDirectory()));
    File testtimes = new File(outputDir, "testtimes.txt");
    assertThat(testtimes.toString(), testtimes, is(anExistingFile()));
  }
}
