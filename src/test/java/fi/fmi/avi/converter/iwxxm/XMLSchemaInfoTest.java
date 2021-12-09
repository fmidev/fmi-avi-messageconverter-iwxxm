package fi.fmi.avi.converter.iwxxm;

import static fi.fmi.avi.converter.iwxxm.XMLSchemaInfo.decodeSchemaLocation;
import static fi.fmi.avi.converter.iwxxm.XMLSchemaInfo.encodeSchemaLocation;
import static org.junit.Assert.assertEquals;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class XMLSchemaInfoTest {
    @Test
    public void decodeSchemaLocation_returns_empty_map_on_empty_input() {
        assertEquals(Collections.emptyMap(), decodeSchemaLocation(""));
    }

    @Test
    public void decodeSchemaLocation_returns_empty_map_on_whitespace_input() {
        assertEquals(Collections.emptyMap(), decodeSchemaLocation("   \n   \t  "));
    }

    @Test
    public void decodeSchemaLocation_returns_namespace_as_key_and_location_as_value_retaining_iteration_order() {
        final Map<String, String> expected = new HashMap<>();
        expected.put("namespace1", "location1");
        expected.put("namespace2", "location2");
        final Map<String, String> actual = decodeSchemaLocation("namespace1  \t  location1 namespace2  \n   location2");
        assertEquals(expected, actual);
    }

    @Test
    public void decodeSchemaLocation_returns_map_retaining_iteration_order() {
        final List<Map.Entry<String, String>> expected = Arrays.asList(//
                new AbstractMap.SimpleImmutableEntry<>("namespace1", "location1"), //
                new AbstractMap.SimpleImmutableEntry<>("namespace2", "location2"));
        final List<Map.Entry<String, String>> actual = new ArrayList<>(decodeSchemaLocation("namespace1 location1 namespace2 location2").entrySet());
        assertEquals(expected, actual);
    }

    @Test
    public void encodeSchemaLocation_returns_empty_input_as_empty_string() {
        assertEquals("", encodeSchemaLocation(Collections.emptyMap()));
    }

    @Test
    public void encodeSchemaLocation_returns_items_separated_by_whitespace_retaining_iteration_order() {
        final Map<String, String> input = new LinkedHashMap<>();
        input.put("namespace1", "location1");
        input.put("namespace2", "location2");
        assertEquals("namespace1 location1 namespace2 location2", encodeSchemaLocation(input));
    }
}
