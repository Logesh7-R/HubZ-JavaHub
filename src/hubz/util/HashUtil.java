package hubz.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtil {

    private HashUtil(){}

    public static String sha256String(String input){
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(input.getBytes());
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    public static String sha256File(String filePath) throws IOException{
        try{
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try(InputStream is = Files.newInputStream(Paths.get(filePath));
                BufferedInputStream bis = new BufferedInputStream(is)) {
                byte[] buffer = new byte[8192];
                int byteread;
                while((byteread= bis.read(buffer))!=-1){
                    md.update(buffer,0,byteread);
                }
                byte[] hashBytes = md.digest();
                return bytesToHex(hashBytes);
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    public static String bytesToHex(byte[] hashBytes){
        StringBuilder hex = new StringBuilder();
        for(byte b:hashBytes){
            hex.append(String.format("%02x",b));
        }
        return hex.toString();
    }
}
