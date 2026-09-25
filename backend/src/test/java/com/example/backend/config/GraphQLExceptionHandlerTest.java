package com.example.backend.config;

import com.example.backend.exception.GameNotFoundException;
import com.example.backend.exception.GameStateException;
import graphql.GraphQLError;
import graphql.execution.ResultPath;
import graphql.schema.DataFetchingEnvironment;
import org.junit.jupiter.api.Test;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Real service methods wrap a validation failure once more before it reaches this handler
 * (GameActionService.placeBet: "Failed to place bet" wrapping the GameStateException from
 * validatePlayerTurn) -- exactly that two-level shape is what gets built and resolved here.
 */
class GraphQLExceptionHandlerTest {

    private final GraphQLExceptionHandler handler = new GraphQLExceptionHandler();
    private final DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);

    {
        graphql.execution.ExecutionStepInfo stepInfo = mock(graphql.execution.ExecutionStepInfo.class);
        when(env.getExecutionStepInfo()).thenReturn(stepInfo);
        when(stepInfo.getPath()).thenReturn(ResultPath.rootPath());
        graphql.language.Field field = graphql.language.Field.newField("dummy").build();
        when(env.getField()).thenReturn(field);
    }

    private ErrorType classificationOf(Throwable outer) {
        GraphQLError error = handler.resolveToSingleError(outer, env);
        return (ErrorType) error.getErrorType();
    }

    @Test
    void aNotYourTurnFailureIsBadRequestNotAGenericInternalError() {
        RuntimeException wrapped = new RuntimeException("Failed to place bet", new GameStateException("Not player's turn"));

        assertEquals(ErrorType.BAD_REQUEST, classificationOf(wrapped));
        assertEquals("Not player's turn", handler.resolveToSingleError(wrapped, env).getMessage());
    }

    @Test
    void aMissingGameIsNotFoundWithoutStringMatchingTheMessage() {
        RuntimeException wrapped = new RuntimeException("Failed to place bet", new GameNotFoundException("Game not found: g1"));

        assertEquals(ErrorType.NOT_FOUND, classificationOf(wrapped));
    }

    @Test
    void accessDeniedStaysForbidden() {
        assertEquals(ErrorType.FORBIDDEN, classificationOf(new AccessDeniedException("Not your seat")));
    }

    @Test
    void aBetValidationFailureStaysBadRequest() {
        assertEquals(ErrorType.BAD_REQUEST, classificationOf(new IllegalArgumentException("Bet amount cannot be negative")));
    }

    @Test
    void anUnrecognisedFailureIsStillAGenericInternalErrorAndDoesNotLeakItsMessage() {
        RuntimeException bug = new RuntimeException("NullPointerException at line 42 in BettingManager.java");

        assertEquals(ErrorType.INTERNAL_ERROR, classificationOf(bug));
        assertEquals("Internal server error", handler.resolveToSingleError(bug, env).getMessage());
    }
}
