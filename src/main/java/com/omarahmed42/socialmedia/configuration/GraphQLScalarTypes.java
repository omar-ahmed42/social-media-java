package com.omarahmed42.socialmedia.configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.scalars.ExtendedScalars;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;

@Configuration
public class GraphQLScalarTypes {

    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder
                .scalar(localDateTimeType())
                .scalar(ExtendedScalars.Date)
                .scalar(ExtendedScalars.GraphQLLong)
                .scalar(ExtendedScalars.NonNegativeInt)
                .scalar(ExtendedScalars.PositiveInt);
    }

    @Bean
    public GraphQLScalarType localDateTimeType() {
        return GraphQLScalarType.newScalar().name("DateTime")
                .description("A scalar for LocalDateTime java type")
                .coercing(new Coercing<LocalDateTime, Object>() {
                    @Override
                    @SuppressWarnings("rawtypes")
                    public LocalDateTime parseLiteral(Value input,
                            CoercedVariables variables,
                            GraphQLContext graphQLContext,
                            Locale locale) throws CoercingParseLiteralException {
                        return parseLocalDateTimeFromLiteral(input);
                    }

                    @Override
                    public LocalDateTime parseValue(Object input,
                            GraphQLContext graphQLContext,
                            Locale locale) throws CoercingParseValueException {

                        return parseLocalDateTimeFromValue(input);
                    }

                    @Override
                    public Object serialize(Object dataFetcherResult,
                            GraphQLContext graphQLContext,
                            Locale locale) throws CoercingSerializeException {
                        return serializeLocalDateTime(dataFetcherResult);
                    }
                }).build();
    }

    private static Object serializeLocalDateTime(Object dataFetcherResult) {
        if (dataFetcherResult instanceof LocalDateTime) {
            LocalDateTime result = (LocalDateTime) dataFetcherResult;
            return result.toString();
        }

        throw new CoercingSerializeException(
                "Unable to serialize " + dataFetcherResult.toString() + " as a local date time");
    }

    private static LocalDateTime parseLocalDateTimeFromValue(Object input) {
        try {
            String localDateTimeText = String.valueOf(input);
            return LocalDateTime.parse(localDateTimeText);
        } catch (DateTimeParseException e) {
            throw new CoercingParseValueException("Unable to parse variable value " + input + " as local date time");
        }
    }

    private static LocalDateTime parseLocalDateTimeFromLiteral(Object input) {
        try {
            String localDateTimeText = ((StringValue) input).getValue();
            return LocalDateTime.parse(localDateTimeText);
        } catch (DateTimeParseException | ClassCastException e) {
            throw new CoercingParseLiteralException("Value is not local date time");
        }
    }
}
