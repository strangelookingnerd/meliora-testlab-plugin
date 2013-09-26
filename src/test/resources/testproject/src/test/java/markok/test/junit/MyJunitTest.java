package markok.test.junit;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * @author Marko Kanala
 */
public class MyJunitTest {

    @Test
    public void testSomethingThatPasses() {
        assertTrue("True was not true", true);
    }

    @Test
    public void testSomethingThatFails() {
        assertTrue("False was not true", false);
    }

    @Test
    public void testSomethingThatFailsToo() {
        assertTrue("False was not true again", false);
    }

}
