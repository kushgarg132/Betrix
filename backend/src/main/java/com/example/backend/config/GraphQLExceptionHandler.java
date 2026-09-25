package com.example.backend.config;

import graphql.GraphQLError;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class GraphQLExceptionHandler extends DataFetcherExceptionResolverAdapter {

    /** "username may only contain ..." from the property name (last path segment) and the violation message. */
    private static String describe(ConstraintViolation<?> v) {
        String path = v.getPropertyPath().toString();
        return path.substring(path.lastIndexOf('.') + 1) + " " + v.getMessage();
    }

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;

        // A resolver that built its own classified error (bad credentials, rate limit) keeps it
        if (ex instanceof GraphQLError error) {
            return error;
        }
        if (cause instanceof GraphQLError error) {
            return error;
        }
        if (ex instanceof ConstraintViolationException || cause instanceof ConstraintViolationException) {
            ConstraintViolationException violations = ex instanceof ConstraintViolationException cve
                    ? cve : (ConstraintViolationException) cause;
            String message = violations.getConstraintViolations().stream()
                    .map(GraphQLExceptionHandler::describe)
                    .sorted()
                    .collect(Collectors.joining("; "));
            return GraphqlErrorBuilder.newError(env)
                    .errorType(ErrorType.BAD_REQUEST)
                    .message(message)
                    .build();
        }
        if (cause instanceof AccessDeniedException) {
            return GraphqlErrorBuilder.newError(env)
                    .errorType(ErrorType.FORBIDDEN)
                    .message("Access denied")
                    .build();
        }
        if (cause instanceof IllegalArgumentException) {
            return GraphqlErrorBuilder.newError(env)
                    .errorType(ErrorType.BAD_REQUEST)
                    .message(cause.getMessage())
                    .build();
        }
        if (cause instanceof RuntimeException && cause.getMessage() != null
                && cause.getMessage().startsWith("Game not found")) {
            return GraphqlErrorBuilder.newError(env)
                    .errorType(ErrorType.NOT_FOUND)
                    .message(cause.getMessage())
                    .build();
        }
        return GraphqlErrorBuilder.newError(env)
                .errorType(ErrorType.INTERNAL_ERROR)
                .message("Internal server error")
                .build();
    }
}
