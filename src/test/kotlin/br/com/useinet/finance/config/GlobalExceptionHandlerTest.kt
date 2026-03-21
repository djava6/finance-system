package br.com.useinet.finance.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException

class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler()

    @Test
    fun handleValidation_shouldReturn400WithFieldErrorMessage() {
        val bindingResult = BeanPropertyBindingResult(Any(), "request")
        bindingResult.addError(FieldError("request", "nome", "Nome é obrigatório"))
        val method = Any::class.java.getDeclaredMethod("toString")
        val methodParam = org.springframework.core.MethodParameter(method, -1)
        val ex = MethodArgumentNotValidException(methodParam, bindingResult)

        val response = handler.handleValidation(ex)

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body!!["erro"]).contains("Nome é obrigatório")
    }

    @Test
    fun handleValidation_shouldJoinMultipleFieldErrors() {
        val bindingResult = BeanPropertyBindingResult(Any(), "request")
        bindingResult.addError(FieldError("request", "nome", "Nome é obrigatório"))
        bindingResult.addError(FieldError("request", "valor", "Valor inválido"))
        val method = Any::class.java.getDeclaredMethod("toString")
        val methodParam = org.springframework.core.MethodParameter(method, -1)
        val ex = MethodArgumentNotValidException(methodParam, bindingResult)

        val response = handler.handleValidation(ex)

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body!!["erro"]).contains("Nome é obrigatório")
        assertThat(response.body!!["erro"]).contains("Valor inválido")
    }

    @Test
    fun handleIllegalArgument_shouldReturn400WithMessage() {
        val response = handler.handleIllegalArgument(IllegalArgumentException("Conta não encontrada"))

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body!!["erro"]).isEqualTo("Conta não encontrada")
    }

    @Test
    fun handleIllegalArgument_shouldReturn400WithDefaultWhenMessageIsNull() {
        val ex = mock(IllegalArgumentException::class.java)
        `when`(ex.message).thenReturn(null)

        val response = handler.handleIllegalArgument(ex)

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body!!["erro"]).isEqualTo("Requisição inválida.")
    }

    @Test
    fun handleAccessDenied_shouldReturn403() {
        val response = handler.handleAccessDenied(AccessDeniedException("forbidden"))

        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        assertThat(response.body!!["erro"]).isEqualTo("Acesso negado.")
    }

    @Test
    fun handleGeneral_shouldReturn500() {
        val response = handler.handleGeneral(RuntimeException("unexpected"))

        assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(response.body!!["erro"]).contains("Erro interno")
    }
}
