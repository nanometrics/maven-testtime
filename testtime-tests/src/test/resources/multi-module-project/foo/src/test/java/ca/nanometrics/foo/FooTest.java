package ca.nanometrics.foo;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FooTest
{
  @Test
  public void testAdd()
  {
    assertEquals(3, new Foo().add(1,2));
  }
}
