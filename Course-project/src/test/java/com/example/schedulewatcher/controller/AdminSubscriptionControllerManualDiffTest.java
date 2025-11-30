package com.example.schedulewatcher.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class AdminSubscriptionControllerManualDiffTest {

    private final ObjectMapper om = new ObjectMapper();

    private String callBuildManualDiff(String oldJson, String newJson) throws Exception {
        Method m = AdminSubscriptionController.class
                .getDeclaredMethod("buildManualDiff", String.class, String.class);
        m.setAccessible(true);
        return (String) m.invoke(null, oldJson, newJson);
    }

    @Test
    void buildManualDiffDetectsRoomChange() throws Exception {

        String oldFilters = "{\"room\":\"101\"}";
        String newFilters = "{\"room\":\"303\"}";

        String diffJson = callBuildManualDiff(oldFilters, newFilters);
        JsonNode diff = om.readTree(diffJson);

        assertEquals("manual", diff.get("type").asText());

        JsonNode roomChange = diff.get("changes").get("room");
        assertNotNull(roomChange);
        assertEquals("101", roomChange.get("from").asText());
        assertEquals("303", roomChange.get("to").asText());
    }

    @Test
    void buildManualDiffIgnoresUnchangedFields() throws Exception {

        String oldFilters = "{\"room\":\"101\",\"teacher\":\"T-4874\"}";
        String newFilters = "{\"room\":\"101\",\"teacher\":\"T-4874\"}";

        String diffJson = callBuildManualDiff(oldFilters, newFilters);
        JsonNode diff = om.readTree(diffJson);

        JsonNode changes = diff.get("changes");
        assertNotNull(changes);

        assertEquals(0, changes.size());
    }
}
