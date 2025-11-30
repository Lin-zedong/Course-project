package com.example.schedulewatcher.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScheduleDiffUtilTest {

    private final ObjectMapper om = new ObjectMapper();

    @Test
    void diffTreatsNewFieldAsNoValueChange() throws Exception {

        String oldJson = "{\"room\":\"101\"}";
        String newJson = "{\"room\":\"101\",\"teacher\":\"T-4874\"}";

        String diffJson = ScheduleDiffUtil.diff(oldJson, newJson);

        JsonNode diff = om.readTree(diffJson);

        assertEquals("changed", diff.get("type").asText());

        assertEquals(0, diff.get("changedFields").asInt());
    }

    @Test
    void diffDetectsChangedRoomAndCountsChangedFields() throws Exception {

        String oldJson = "{\"room\":\"101\",\"teacher\":\"T-4874\"}";
        String newJson = "{\"room\":\"303\",\"teacher\":\"T-4874\"}";

        String diffJson = ScheduleDiffUtil.diff(oldJson, newJson);
        JsonNode diff = om.readTree(diffJson);

        assertEquals("changed", diff.get("type").asText());

        assertEquals(1, diff.get("changedFields").asInt());

        JsonNode roomChange = diff.get("room");
        assertNotNull(roomChange);
        assertEquals("101", roomChange.get("old").asText());
        assertEquals("303", roomChange.get("new").asText());
    }

    @Test
    void diffDetectsMultipleFieldChanges() throws Exception {

        String oldJson = "{\"room\":\"101\",\"teacher\":\"T-4874\"}";
        String newJson = "{\"room\":\"303\",\"teacher\":\"T-4877\"}";

        String diffJson = ScheduleDiffUtil.diff(oldJson, newJson);
        JsonNode diff = om.readTree(diffJson);

        assertEquals("changed", diff.get("type").asText());
        
        assertTrue(diff.get("changedFields").asInt() >= 2);
    }

    @Test
    void sha256_sameInputSameHash_differentInputDifferentHash() {
        String h1 = ScheduleDiffUtil.sha256("abc");
        String h2 = ScheduleDiffUtil.sha256("abc");
        String h3 = ScheduleDiffUtil.sha256("def");

        assertEquals(h1, h2);
        assertNotEquals(h1, h3);
    }

}