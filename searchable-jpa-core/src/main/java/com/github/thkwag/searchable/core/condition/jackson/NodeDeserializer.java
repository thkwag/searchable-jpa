package com.github.thkwag.searchable.core.condition.jackson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.thkwag.searchable.core.condition.SearchCondition.Condition;
import com.github.thkwag.searchable.core.condition.SearchCondition.Group;
import com.github.thkwag.searchable.core.condition.SearchCondition.Node;
import com.github.thkwag.searchable.core.condition.operator.LogicalOperator;
import com.github.thkwag.searchable.core.condition.operator.SearchOperator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Custom deserializer for Node interface implementations.
 * Handles deserialization of both Condition and Group nodes.
 */
@SuppressWarnings({"UnusedReturnValue", "unused"})
public class NodeDeserializer<D> extends JsonDeserializer<Node> {
    @JsonCreator
    public NodeDeserializer() {
    }

    @Override
    public Node deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        JsonNode node = mapper.readTree(p);

        return node.has("conditions") ? deserializeGroup(node, mapper) : deserializeCondition(node);
    }

    private Group deserializeGroup(JsonNode node, ObjectMapper mapper) throws IOException {
        LogicalOperator operator = getLogicalOperator(node);
        List<Node> nodes = new ArrayList<>();

        JsonNode conditions = node.get("conditions");
        if (conditions.isArray()) {
            for (JsonNode condition : conditions) {
                Node deserializedNode = deserialize(mapper.treeAsTokens(condition), null);
                if (deserializedNode instanceof Group) {
                    Group group = (Group) deserializedNode;
                    if (group.getNodes().size() == 1 ||
                            group.getNodes().stream().allMatch(n -> Objects.equals(n.getOperator(), group.getOperator()))) {
                        nodes.addAll(group.getNodes());
                    } else {
                        nodes.add(group);
                    }
                } else {
                    nodes.add(deserializedNode);
                }
            }
        }

        if (nodes.size() == 1 && operator == null) {
            return new Group(null, nodes);
        }

        boolean allSameOperator = nodes.stream()
                .skip(1)
                .allMatch(n -> Objects.equals(n.getOperator(), nodes.get(0).getOperator()));

        if (allSameOperator) {
            return new Group(operator, nodes);
        }

        return new Group(operator, nodes);
    }

    private Node deserializeCondition(JsonNode node) {
        LogicalOperator operator = node.has("operator") ?
                LogicalOperator.valueOf(node.get("operator").asText().toUpperCase()) : null;

        String field = node.get("field").asText();
        SearchOperator searchOperator = SearchOperator.fromName(node.get("searchOperator").asText());
        String entityField = node.has("entityField") ? node.get("entityField").asText() : null;

        JsonNode valueNode = node.get("value");
        Object value = convertValue(valueNode);

        JsonNode value2Node = node.has("value2") ? node.get("value2") : null;
        Object value2 = convertValue(value2Node);

        return new Condition(operator, field, searchOperator, value, value2, entityField);
    }

    private LogicalOperator getLogicalOperator(JsonNode node) {
        return node.has("operator") ? LogicalOperator.fromName(node.get("operator").asText()) : null;
    }

    private Object convertValue(JsonNode valueNode) {
        if (valueNode == null) return null;

        if (valueNode.isNumber()) {
            if (valueNode.isLong() || valueNode.isBigInteger()) {
                return valueNode.asLong();
            } else if (valueNode.isDouble() || valueNode.isBigDecimal()) {
                return valueNode.asDouble();
            }
            return valueNode.asLong();
        }

        return valueNode.isBoolean() ? valueNode.asBoolean() : valueNode.asText();
    }
} 