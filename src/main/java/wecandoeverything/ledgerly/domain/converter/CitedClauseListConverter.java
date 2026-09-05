package wecandoeverything.ledgerly.domain.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import wecandoeverything.ledgerly.domain.CitedClause;

import java.util.List;

@Converter
public class CitedClauseListConverter implements AttributeConverter<List<CitedClause>, String> {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<CitedClause> attribute) {
        try {
            return attribute == null ? null : mapper.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize cited clauses", e);
        }
    }

    @Override
    public List<CitedClause> convertToEntityAttribute(String dbData) {
        try {
            return dbData == null ? List.of()
                    : mapper.readValue(dbData, mapper.getTypeFactory()
                            .constructCollectionType(List.class, CitedClause.class));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize cited clauses: " + dbData, e);
        }
    }
}