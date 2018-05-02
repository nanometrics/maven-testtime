package ca.nanometrics.bar;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BarTest
{
  @Test
  public void testSubtract()
  {
    assertEquals(1, Bar.subtract(2,1));
  }
}
