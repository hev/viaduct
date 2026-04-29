package com.example.amazonreviews.service.viaduct

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import kotlinx.coroutines.future.await
import viaduct.service.api.ExecutionInput
import viaduct.service.api.ExecutionResult
import viaduct.service.api.Viaduct

private const val QUERY_FIELD = "query"
private const val VARIABLES_FIELD = "variables"

@Controller
class ViaductRestController(
    private val viaduct: Viaduct,
) {
    @Post("/graphql")
    suspend fun graphql(
        @Body request: Map<String, Any>,
    ): HttpResponse<Map<String, Any?>> {
        val executionInput = createExecutionInput(request)
        val result = viaduct.executeAsync(executionInput, DEFAULT_SCHEMA_ID).await()
        return HttpResponse.status<Map<String, Any?>>(statusCode(result)).body(result.toSpecification())
    }

    private fun createExecutionInput(request: Map<String, Any>): ExecutionInput {
        @Suppress("UNCHECKED_CAST")
        return ExecutionInput.create(
            operationText = request[QUERY_FIELD] as String,
            variables = (request[VARIABLES_FIELD] as? Map<String, Any>) ?: emptyMap(),
            requestContext = emptyMap<String, Any>(),
        )
    }

    private fun statusCode(result: ExecutionResult) =
        when {
            result.errors.isNotEmpty() -> HttpStatus.BAD_REQUEST
            else -> HttpStatus.OK
        }
}
