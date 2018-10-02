package org.gaul.modernizer_maven_plugin;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;


public class UtilsTest {

    @Test
    public void testFilterComments() {
        Collection<String> lines = Arrays.asList("foo", "", " # comment", " bar ");
        assertEquals(Arrays.asList("foo", " bar "), Utils.filterCommentLines(lines));
    }
}
