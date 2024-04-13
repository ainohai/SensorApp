package fi.ainon.polarAppis.worker

import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

//TODO: Not in use yet.
/**
 * Note: this is duplicate from the server side. Wouldn't hurt thinking about alternatives and the cost.
 */
interface MessageCipher {
    fun genKey(): SecretKey
    fun saveSecretKeyToFile(secretKey: SecretKey, fileName: String)
    fun readSecretKeyFromFile(fileName: String): SecretKey
    fun generateFixedIV(size: Int): ByteArray
    fun encryptData(origText: String, key: SecretKey, ivLength: Int): ByteArray
    fun decryptData(encryptedDataWithIv: ByteArray, key: SecretKey, ivLength: Int): ByteArray
}

class DefaultCipher () : MessageCipher {

    private val ALGORITHM = "AES"


    override fun genKey(): SecretKey {
        val keygen = KeyGenerator.getInstance(ALGORITHM)
        keygen.init(256)
        val key: SecretKey = keygen.generateKey()
        return key
    }

    override fun saveSecretKeyToFile(secretKey: SecretKey, fileName: String) {
        val fileOutputStream = FileOutputStream(fileName)
        val objectOutputStream = ObjectOutputStream(fileOutputStream)
        objectOutputStream.writeObject(secretKey.encoded)
        objectOutputStream.close()
        fileOutputStream.close()
    }

    override fun readSecretKeyFromFile(fileName: String): SecretKey {
        val fileInputStream = FileInputStream(fileName)
        val objectInputStream = ObjectInputStream(fileInputStream)
        val encodedKey = objectInputStream.readObject() as ByteArray
        objectInputStream.close()
        fileInputStream.close()

        return SecretKeySpec(encodedKey, ALGORITHM)
    }

    override fun generateFixedIV(size: Int): ByteArray {
        val random = SecureRandom()
        val iv = ByteArray(size)
        random.nextBytes(iv)
        return iv
    }

    override fun encryptData(origText: String, key: SecretKey, ivLength: Int): ByteArray {

        val iv = generateFixedIV(ivLength)

        val plaintext: ByteArray = origText.toByteArray(Charsets.UTF_16)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
        val ivSpec = IvParameterSpec(iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec)
        val ciphertext: ByteArray = cipher.doFinal(plaintext)

        return cipher.iv + ciphertext
    }

    override fun decryptData(encryptedDataWithIv: ByteArray, key: SecretKey, ivLength: Int): ByteArray {

        val encryptedData = encryptedDataWithIv.copyOfRange(ivLength, encryptedDataWithIv.size)
        val iv = encryptedDataWithIv.copyOfRange(0, ivLength)

        val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
        val ivSpec = IvParameterSpec(iv)
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)
        return cipher.doFinal(encryptedData)
    }
}