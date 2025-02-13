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
import com.github.thkwag.searchable.core.exception.SearchableValidationException;
import com.github.thkwag.searchable.core.i18n.MessageUtils;
import com.github.thkwag.searchable.core.utils.SearchableValueParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Custom deserializer for Node interface implementations.
 * Handles deserialization of both Condition and Group nodes.
 */
public class NodeDeserializer extends JsonDeserializer<Node> {
    private static final Logger log = LoggerFactory.getLogger(NodeDeserializer.class);
    private final Map<String, Class<?>> fieldTypeCache = new HashMap<>();
    private Class<?> dtoClass;  // DTO 클래스 정보 저장

    public void setDtoClass(Class<?> dtoClass) {
        this.dtoClass = dtoClass;
    }

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
                Node deserializedNode = condition.has("conditions") ? 
                    mapper.treeToValue(condition, Group.class) : 
                    mapper.treeToValue(condition, Condition.class);
                
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

        return new Group(operator, nodes);
    }

    private Node deserializeCondition(JsonNode node) {
        LogicalOperator operator = node.has("operator") ?
                LogicalOperator.valueOf(node.get("operator").asText().toUpperCase()) : null;

        String field = node.get("field").asText();
        SearchOperator searchOperator = SearchOperator.fromName(node.get("searchOperator").asText());
        String entityField = node.has("entityField") ? node.get("entityField").asText() : null;

        if (searchOperator == null) {
            throw new SearchableValidationException(MessageUtils.getMessage("validator.field.not.supported", 
                new Object[]{node.get("searchOperator").asText()}));
        }

        JsonNode valueNode = node.get("value");
        Object value = getNodeValue(valueNode);

        JsonNode value2Node = node.has("value2") ? node.get("value2") : null;
        Object value2 = getNodeValue(value2Node);

        return new Condition(operator, field, searchOperator, value, value2, entityField);
    }

    private Object getNodeValue(JsonNode node) {
        if (node == null) return null;
        return node.asText();
    }

    private LogicalOperator getLogicalOperator(JsonNode node) {
        return node.has("operator") ? LogicalOperator.fromName(node.get("operator").asText()) : null;
    }

} 