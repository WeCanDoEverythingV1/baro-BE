package wecandoeverything.ledgerly.domain.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import wecandoeverything.ledgerly.domain.RuleConditions;

@Converter
public class RuleConditionsConverter implements AttributeConverter<RuleConditions, String> {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(RuleConditions attribute) {
        try {
            return attribute == null ? null : mapper.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize conditions", e);
        }
    }

    @Override
    public RuleConditions convertToEntityAttribute(String dbData) {
        try {
            return dbData == null ? null : mapper.readValue(dbData, RuleConditions.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize conditions: " + dbData, e);
        }
    }
}