package br.com.useinet.finance.config

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Component
@Converter
class AesAttributeConverter(
    @Value("\${app.encryption.key:}") private val keyBase64: String
) : AttributeConverter<String, String> {

    private val key: SecretKey by lazy {
        require(keyBase64.isNotBlank()) {
            "app.encryption.key não configurada. Gere uma chave com: openssl rand -base64 32"
        }
        SecretKeySpec(Base64.getDecoder().decode(keyBase64.trim()), "AES")
    }

    override fun convertToDatabaseColumn(attribute: String?): String? {
        attribute ?: return null
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(attribute.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(iv + encrypted)
    }

    override fun convertToEntityAttribute(dbData: String?): String? {
        dbData ?: return null
        val decoded = Base64.getDecoder().decode(dbData)
        val iv = decoded.sliceArray(0..11)
        val cipherText = decoded.sliceArray(12..decoded.lastIndex)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        return String(cipher.doFinal(cipherText), Charsets.UTF_8)
    }
}
