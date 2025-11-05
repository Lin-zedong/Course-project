package com.example.schedulewatcher.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ScheduleDiffUtil {
    private static final ObjectMapper om = new ObjectMapper();

    public static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(s.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b: hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(s.hashCode());
        }
    }

    public static String diff(String oldJson, String newJson) {
        try {
            JsonNode oldNode = (oldJson == null || oldJson.isBlank()) ? null : om.readTree(oldJson);
            JsonNode newNode = om.readTree(newJson);

            if (oldNode == null) {
                return om.createObjectNode().put("type","initial").toString();
            }

            var out = om.createObjectNode();
            out.put("type","changed");

            if (oldNode.isObject() && newNode.isObject()) {
                Set<String> keys = new HashSet<>();
                oldNode.fieldNames().forEachRemaining(keys::add);
                newNode.fieldNames().forEachRemaining(keys::add);

                int changed = 0;
                for (String k : keys) {
                    JsonNode ov = oldNode.get(k);
                    JsonNode nv = newNode.get(k);
                    if (ov == null || nv == null) continue;

                    if (ov.isValueNode() && nv.isValueNode() && !ov.equals(nv)) {
                        var pair = om.createObjectNode();
                        pair.set("old", ov);
                        pair.set("new", nv);
                        out.set(k, pair);
                        changed++;
                    }
                }
                out.put("changedFields", changed);
                return out.toString();
            }

            return om.createObjectNode().put("type","changed").toString();
        } catch (Exception e) {
            return "{\"type\":\"changed\"}";
        }
    }
}
