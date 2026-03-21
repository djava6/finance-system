package br.com.useinet.finance.config

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.Base64

class AesAttributeConverterTest {

    // 256-bit AES key (32 bytes) encoded in Base64 — fixed test key
    private val testKey = Base64.getEncoder().encodeToString(ByteArray(32) { it.toByte() })
    private val converter = AesAttributeConverter(testKey)

    @Test
    fun convertToDatabaseColumn_shouldReturnNullForNullInput() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull()
    }

    @Test
    fun convertToEntityAttribute_shouldReturnNullForNullInput() {
        assertThat(converter.convertToEntityAttribute(null)).isNull()
    }

    @Test
    fun convertToDatabaseColumn_shouldReturnBase64EncodedString() {
        val encrypted = converter.convertToDatabaseColumn("hello")
        assertThat(encrypted).isNotNull
        // must be valid Base64 — no exception means valid
        Base64.getDecoder().decode(encrypted)
    }

    @Test
    fun convertToDatabaseColumn_shouldProduceDifferentCiphertextEachCall() {
        // each encryption uses a random IV
        val e1 = converter.convertToDatabaseColumn("same")
        val e2 = converter.convertToDatabaseColumn("same")
        assertThat(e1).isNotEqualTo(e2)
    }

    @Test
    fun roundTrip_shouldDecryptToOriginalValue() {
        val original = "minha conta corrente"
        val encrypted = converter.convertToDatabaseColumn(original)
        val decrypted = converter.convertToEntityAttribute(encrypted)
        assertThat(decrypted).isEqualTo(original)
    }

    @Test
    fun roundTrip_shouldHandleSpecialCharacters() {
        val original = "Agência: 0001 / Conta: 12345-6 @#\$"
        val encrypted = converter.convertToDatabaseColumn(original)
        val decrypted = converter.convertToEntityAttribute(encrypted)
        assertThat(decrypted).isEqualTo(original)
    }

    @Test
    fun roundTrip_shouldHandleEmptyString() {
        val encrypted = converter.convertToDatabaseColumn("")
        val decrypted = converter.convertToEntityAttribute(encrypted)
        assertThat(decrypted).isEqualTo("")
    }

    @Test
    fun constructor_shouldThrowWhenKeyIsBlank() {
        val converterBlank = AesAttributeConverter("")
        assertThatThrownBy { converterBlank.convertToDatabaseColumn("test") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("app.encryption.key")
    }
}
