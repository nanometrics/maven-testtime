package ca.nanometrics.maven.testtime;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestTimes
{
  public static final String LOG_LIMIT_PROPERTY = "testtime.loglimit";
  public static final String FILE_LIMIT_PROPERTY = "testtime.filelimit";
  public static final String FILE_NAME = "testtimes.txt";

  private static final Pattern PATTERN =
      Pattern.compile("^.*<test(suite|case).* name=\"([^\"]+)\"(.* classname=\"([^\"]+)\")*.* time=\"([0-9.]+)\"");

  private final List<TestTime> m_testTimes = new LinkedList<>();
  private final int m_resultsLogLimit;
  private final int m_resultsFileLimit;
  private final Consumer<String> m_log;
  private int m_countTestReportDirectories;
  private int m_countFiles;
  private long m_startTime;
  private long m_endTime;

  public TestTimes(Consumer<String> log, int resultsLogLimit, int resultsFileLimit)
  {
    m_log = log;
    m_resultsLogLimit = resultsLogLimit;
    m_resultsFileLimit = resultsFileLimit;
  }

  public void processBuildDirectories(Path outputFolder, Collection<String> buildDirectories)
  {
    m_startTime = System.currentTimeMillis();
    Path surefireReportPath = Paths.get("surefire-reports");
    Path failsafeReportPath = Paths.get("failsafe-reports");
    buildDirectories.stream().forEach(buildDirectory -> {
      try
      {
        Files.walk(Paths.get(buildDirectory)).filter(Files::isDirectory)
            .filter(path -> path.endsWith(surefireReportPath) || path.endsWith(failsafeReportPath))
            .forEach(path -> processTestReportDirectory(path));
      }
      catch (IOException e)
      {
        m_log.accept("Could not process " + buildDirectory + " - " + e.getMessage());
      }
    });
    m_endTime = System.currentTimeMillis();
    outputResults(outputFolder);
  }

  private void outputResults(Path outputFolder)
  {
    Collections.sort(m_testTimes);
    logResults(m_log, m_resultsLogLimit);
    writeResultsToFile(outputFolder);
  }

  private void writeResultsToFile(Path outputFolder)
  {
    if (outputFolder != null)
    {
      try (PrintStream output = new PrintStream(outputFolder.resolve(FILE_NAME).toFile()))
      {
        logResults(a -> output.println(a), m_resultsFileLimit);
        output.println(String.format("Processed %d directories and %d files in %.2f s", m_countTestReportDirectories,
            m_countFiles, (m_endTime - m_startTime) / 1000.0));
      }
      catch (FileNotFoundException e)
      {
        m_log.accept("Could not write to " + outputFolder);
      }
    }
  }

  private void logResults(Consumer<String> log, int resultsLimit)
  {
    if (resultsLimit == 0)
    {
      return;
    }
    log.accept("");
    log.accept("Slowest Test Suites");
    log.accept("===================");
    m_testTimes.stream().filter(testTime -> testTime.isSuite() && testTime.time() != 0.0).limit(resultsLimit)
        .map(TestTime::toString).forEach(log::accept);
    log.accept("--------");
    log.accept(String.format("%8.3f  Total", m_testTimes.stream().filter(testTime -> testTime.isSuite())
        .map(testTime -> testTime.time()).reduce(0.0, Double::sum)));
    log.accept("");
    log.accept("Slowest Test Cases");
    log.accept("==================");
    m_testTimes.stream().filter(testTime -> testTime.isCase() && testTime.time() != 0.0).limit(resultsLimit)
        .map(TestTime::toString).forEach(log::accept);
    log.accept("--------");
    log.accept(String.format("%8.3f  Total", m_testTimes.stream().filter(testTime -> testTime.isCase())
        .map(testTime -> testTime.time()).reduce(0.0, Double::sum)));
    log.accept("");
    log.accept("Note: Suite total can be less than case total when tests are run in parallel.");
  }

  private void processTestReportDirectory(Path directoryPath)
  {
    m_countTestReportDirectories++;
    if (Files.isDirectory(directoryPath))
    {
      try (DirectoryStream<Path> stream = Files.newDirectoryStream(directoryPath, "*.xml"))
      {
        stream.forEach(path -> processTestReport(path));
      }
      catch (IOException e)
      {
        throw new RuntimeException(e);
      }
    }
  }

  private void processTestReport(Path path)
  {
    m_countFiles++;
    try
    {
      for (String line : Files.readAllLines(path))
      {
        Matcher matcher = PATTERN.matcher(line);
        if (matcher.find())
        {
          String name;
          String time;
          if (matcher.group(4) != null)
          {
            name = matcher.group(4) + "." + matcher.group(2);
            time = matcher.group(5);
          }
          else
          {
            name = matcher.group(2);
            time = matcher.group(5);
          }
          TestTime testTime = new TestTime(matcher.group(1), name, Double.parseDouble(time));
          m_testTimes.add(testTime);
        }
      }
    }
    catch (NumberFormatException | IOException e)
    {
      e.printStackTrace();
    }
  }

  static class TestTime implements Comparable<TestTime>
  {
    private final String m_type;
    private final String m_name;
    private final double m_time;

    TestTime(String type, String name, double time)
    {
      m_type = type;
      m_time = time;
      m_name = name;
    }

    boolean isSuite()
    {
      return m_type.equals("suite");
    }

    boolean isCase()
    {
      return m_type.equals("case");
    }

    double time()
    {
      return m_time;
    }

    @Override
    public String toString()
    {
      return String.format("%8.3f  %s", m_time, m_name);
    }

    @Override
    public int compareTo(TestTime that)
    {
      return Double.compare(that.m_time, this.m_time);
    }
  }
}
