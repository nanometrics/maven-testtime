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

public class TestTimeTest
{
  @Test
  public void testBasics() throws Exception
  {
    File destination = new File("target/test-projects");
    File projectDir = ResourceExtractor.extractResourcePath(getClass(), "/multi-module-project", destination, true);
    System.out.println(projectDir.getAbsolutePath());
    assertThat(projectDir, is(notNullValue()));
    assertThat(projectDir.exists(), is(true));

    Verifier verifier = new Verifier(projectDir.getAbsolutePath());

    List<String> cliOptions = new ArrayList<>();

    File localRepo = new File(destination, "local-repo");
    localRepo.mkdirs();
    cliOptions.add("-Dmaven.repo.local=\"" + localRepo.getAbsolutePath() + "\"");
    cliOptions.add("-X");
    verifier.setCliOptions(cliOptions);
    verifier.setDebug(true);
    verifier.executeGoal("verify");

    verifier.verifyErrorFreeLog();
    verifier.verifyTextInLog("Slowest test times for all modules written to");
    verifier.resetStreams();

    File outputDir = new File(verifier.getBasedir(), "target");
    assertThat(outputDir.toString(), outputDir, is(anExistingDirectory()));
    File testtimes = new File(outputDir, "testtimes.txt");
    assertThat(testtimes.toString(), testtimes, is(anExistingFile()));
  }
}
