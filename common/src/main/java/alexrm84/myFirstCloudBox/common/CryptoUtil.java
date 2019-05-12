package alexrm84.myFirstCloudBox.common;

import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;

public class CryptoUtil {
    private static final Logger logger = LogManager.getLogger(CryptoUtil.class);
    private KeyGenerator keyGenerator;
    private SecureRandom secureRandom;
    private final int KEY_BIT_SIZE = 256;
    @Getter @Setter
    private SecretKey secretKeyAES;
    private Cipher cipher;
    private IvParameterSpec ivSpec;

    private KeyPairGenerator keyPairGenerator;
    @Getter @Setter
    private KeyPair keyPairRSA;

    public CryptoUtil() {
        secretKeyAES = null;
        keyPairRSA = null;
        this.secureRandom = new SecureRandom();
        byte[] iv = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        ivSpec = new IvParameterSpec(iv);
    }

    public void initAES() {
        try {
            keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(KEY_BIT_SIZE, secureRandom);
            secretKeyAES = keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            logger.log(Level.ERROR, "Initialize AES  error: ", e);
        }
    }

    public byte[] encryptAES(byte[] data) {
        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeyAES, ivSpec);
            return cipher.doFinal(data);
        } catch (NoSuchAlgorithmException e) {
            logger.log(Level.ERROR, "RSA encryption error: ", e);
        } catch (InvalidKeyException e) {
            logger.log(Level.ERROR, "RSA encryption error: ", e);
        } catch (IllegalBlockSizeException e) {
            logger.log(Level.ERROR, "RSA encryption error: ", e);
        } catch (NoSuchPaddingException e) {
            logger.log(Level.ERROR, "RSA encryption error: ", e);
        } catch (BadPaddingException e) {
            logger.log(Level.ERROR, "RSA encryption error: ", e);
        } catch (InvalidAlgorithmParameterException e) {
            logger.log(Level.ERROR, "RSA encryption error: ", e);
        }
        return null;
    }

    public byte[] decryptAES(byte[] data) {
        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKeyAES, ivSpec);
            return cipher.doFinal(data);
        } catch (NoSuchAlgorithmException e) {
            logger.log(Level.ERROR, "AES decryption error: ", e);
        } catch (InvalidKeyException e) {
            logger.log(Level.ERROR, "AES decryption error: ", e);
        } catch (IllegalBlockSizeException e) {
            logger.log(Level.ERROR, "AES decryption error: ", e);
        } catch (NoSuchPaddingException e) {
            logger.log(Level.ERROR, "AES decryption error: ", e);
        } catch (BadPaddingException e) {
            logger.log(Level.ERROR, "AES decryption error: ", e);
        } catch (InvalidAlgorithmParameterException e) {
            logger.log(Level.ERROR, "AES decryption error: ", e);
        }
        return null;
    }

    public void initRSA() {
        try {
            keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairRSA = keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            logger.log(Level.ERROR, "Initialize RSA  error: ", e);
        }
    }

    public byte[] encryptRSA(SecretKey secretKey) {
        try {
            cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keyPairRSA.getPublic());
            return cipher.doFinal(secretKey.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            logger.log(Level.ERROR, "RSA encryption error: ", e);
        } catch (InvalidKeyException e) {
            logger.log(Level.ERROR, "RSA encryption error: ", e);
        } catch (IllegalBlockSizeException e) {
            logger.log(Level.ERROR, "RSA encryption error: ", e);
        } catch (NoSuchPaddingException e) {
            logger.log(Level.ERROR, "RSA encryption error: ", e);
        } catch (BadPaddingException e) {
            logger.log(Level.ERROR, "RSA encryption error: ", e);
        }
        return null;
    }

    public void decryptRSA(byte[] data) {
        try {
            cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, keyPairRSA.getPrivate());
            this.secretKeyAES = new SecretKeySpec(cipher.doFinal(data), "AES");
            System.out.println("полученный АЕС:  "+secretKeyAES);
        } catch (NoSuchAlgorithmException e) {
            logger.log(Level.ERROR, "RSA decryption error: ", e);
        } catch (InvalidKeyException e) {
            logger.log(Level.ERROR, "RSA decryption error: ", e);
        } catch (IllegalBlockSizeException e) {
            logger.log(Level.ERROR, "RSA decryption error: ", e);
        } catch (NoSuchPaddingException e) {
            logger.log(Level.ERROR, "RSA decryption error: ", e);
        } catch (BadPaddingException e) {
            logger.log(Level.ERROR, "RSA decryption error: ", e);
        }
    }
}
