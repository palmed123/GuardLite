package cnm.obsoverlay.utils.auth;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javax.crypto.Cipher;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 软件登录客户端
 * 提供软件登录和心跳功能
 */
public class AuthClient {

    private final String serverUrl;
    private final String clientToServerPublicKey;
    private final String serverToClientPublicKey;
    private final Integer softwareId;
    private final Gson gson;

    /**
     * 构造函数
     *
     * @param clientToServerPublicKey 客户端到服务端的公钥
     * @param serverToClientPublicKey 服务端到客户端的公钥
     * @param serverUrl               服务器地址
     * @param softwareId              软件ID
     */
    public AuthClient(String clientToServerPublicKey,
                      String serverToClientPublicKey,
                      String serverUrl,
                      Integer softwareId) {
        this.clientToServerPublicKey = clientToServerPublicKey;
        this.serverToClientPublicKey = serverToClientPublicKey;
        this.serverUrl = serverUrl;
        this.softwareId = softwareId;
        this.gson = new Gson();
    }

    /**
     * 软件登录
     *
     * @param hwid      硬件ID
     * @param phone     手机号
     * @param qqNumbers QQ号列表
     * @return 登录结果
     */
    public LoginResponse login(String hwid, String phone, List<String> qqNumbers) {
        try {
            // 构造请求数据
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("hwid", hwid);
            requestData.put("phone", phone);
            requestData.put("softwareId", this.softwareId);

            // 添加所有QQ号
            for (int i = 0; i < qqNumbers.size(); i++) {
                requestData.put("qq" + (i + 1), qqNumbers.get(i));
            }

            // 构造最终请求JSON：{时间戳, 请求数据}
            Map<String, Object> finalRequest = new HashMap<>();
            finalRequest.put("timestamp", getNetworkTimestamp());
            finalRequest.put("data", requestData);

            String requestJson = gson.toJson(finalRequest);

            // 用RSA公钥加密整个请求
            String encryptedRequest = rsaEncrypt(requestJson);

            // 发送请求
            System.out.println(encryptedRequest);
            String response = sendHttpRequest(serverUrl + "/user/software-login", encryptedRequest);
            System.out.println(response);

            // 解析响应
            return parseLoginResponse(response);

        } catch (Exception e) {
            return new LoginResponse(false, "登录失败: " + e.getMessage());
        }
    }

    /**
     * 心跳请求
     *
     * @param token    令牌
     * @param username 用户名
     * @return 心跳结果
     */
    public LoginResponse heartbeat(String token, String username) {
        try {
            // 构造请求数据
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("token", token);
            requestData.put("username", username);

            // 构造最终请求JSON：{时间戳, 请求数据}
            Map<String, Object> finalRequest = new HashMap<>();
            finalRequest.put("timestamp", getNetworkTimestamp());
            finalRequest.put("data", requestData);

            String requestJson = gson.toJson(finalRequest);

            // 用RSA公钥加密整个请求
            String encryptedRequest = rsaEncrypt(requestJson);

            // 发送请求
            String response = sendHttpRequest(serverUrl + "/user/heartbeat", encryptedRequest);

            // 解析响应
            return parseLoginResponse(response);

        } catch (Exception e) {
            return new LoginResponse(false, "心跳失败: " + e.getMessage());
        }
    }

    /**
     * RSA加密（分块加密支持大数据）
     */
    private String rsaEncrypt(String data) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(clientToServerPublicKey);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(keySpec);

        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);

        // RSA 2048位密钥最大加密245字节，需要分块处理
        int maxBlockSize = 245;
        if (dataBytes.length <= maxBlockSize) {
            // 数据小于最大块大小，直接加密
            byte[] encryptedBytes = cipher.doFinal(dataBytes);
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } else {
            // 数据太大，分块加密
            StringBuilder result = new StringBuilder();
            int offset = 0;

            while (offset < dataBytes.length) {
                int blockSize = Math.min(maxBlockSize, dataBytes.length - offset);
                byte[] block = new byte[blockSize];
                System.arraycopy(dataBytes, offset, block, 0, blockSize);

                byte[] encryptedBlock = cipher.doFinal(block);
                result.append(Base64.getEncoder().encodeToString(encryptedBlock));

                if (offset + blockSize < dataBytes.length) {
                    result.append("|"); // 分块分隔符
                }

                offset += blockSize;
            }

            return result.toString();
        }
    }

    /**
     * RSA验证（解密签名）- 支持分块解密
     */
    private String rsaVerifySignature(String signedData) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(serverToClientPublicKey);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(keySpec);

        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, publicKey);

        // 检查是否是分块数据
        if (signedData.contains("|")) {
            // 分块解密
            String[] blocks = signedData.split("\\|");
            StringBuilder result = new StringBuilder();

            for (String block : blocks) {
                byte[] encryptedBlock = Base64.getDecoder().decode(block);
                byte[] decryptedBlock = cipher.doFinal(encryptedBlock);
                result.append(new String(decryptedBlock, StandardCharsets.UTF_8));
            }

            return result.toString();
        } else {
            // 单块解密
            byte[] encryptedBytes = Base64.getDecoder().decode(signedData);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        }
    }

    /**
     * 获取网络时间戳
     */
    private long getNetworkTimestamp() {
        try {
            // 使用HTTP头获取网络时间
            URL url = URI.create("https://www.baidu.com").toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(3000); // 3秒连接超时
            connection.setReadTimeout(3000);    // 3秒读取超时
            connection.connect();

            // 获取服务器时间
            long serverTime = connection.getDate();
            if (serverTime > 0) {
                return serverTime / 1000;
            }

            // 如果无法获取服务器时间，降级到本地时间
            return 0;

        } catch (Exception e) {
            // 网络请求失败，使用本地时间戳作为降级方案
            return 0;
        }
    }

    /**
     * 发送HTTP请求
     */
    private String sendHttpRequest(String urlString, String requestBody) throws Exception {
        URL url = URI.create(urlString).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        // 发送请求体
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        // 读取响应
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }

        return response.toString();
    }

    /**
     * 解析登录响应
     */
    private LoginResponse parseLoginResponse(String response) {
        try {
            Map<String, Object> result = gson.fromJson(response, new TypeToken<Map<String, Object>>() {
            }.getType());

            // 安全地获取ok字段（服务端Result类使用的是ok而不是success）
            Object successObj = result.get("ok");
            boolean success = false;
            if (successObj instanceof Boolean) {
                success = (Boolean) successObj;
            } else if (successObj instanceof String) {
                success = Boolean.parseBoolean((String) successObj);
            } else if (successObj instanceof Number) {
                success = ((Number) successObj).intValue() != 0;
            }

            String message = (String) result.get("message");
            if (message == null) {
                message = "未知响应";
            }

            if (!success) {
                return new LoginResponse(false, message);
            }

            // 解密响应数据
            String encryptedData = (String) result.get("data");
            if (encryptedData == null) {
                return new LoginResponse(false, "响应数据为空");
            }

            String decryptedData = rsaVerifySignature(encryptedData);

            // 直接解析解密后的JSON
            Map<String, Object> responseData = gson.fromJson(decryptedData, new TypeToken<Map<String, Object>>() {
            }.getType());

            String token = (String) responseData.get("token");
            String username = (String) responseData.get("username");
            Object timestampObj = responseData.get("timestamp");

            if (token == null || username == null || timestampObj == null) {
                return new LoginResponse(false, "响应数据不完整");
            }

            // 验证时间戳（±1误差）
            long responseTimestamp = ((Number) timestampObj).longValue();
            long currentTimestamp = getNetworkTimestamp() / 10;  // 当前网络时间戳/10
            if (currentTimestamp == 0) return new LoginResponse(false, "获取时间戳失败");

            if (Math.abs(currentTimestamp - responseTimestamp) > 1) {
                return new LoginResponse(false, "响应时间戳验证失败");
            }

            return new LoginResponse(true, message, token, username);

        } catch (Exception e) {
            return new LoginResponse(false, "解析响应失败: " + e.getMessage());
        }
    }

    /**
     * 登录响应结果
     */
    public static class LoginResponse {
        private final boolean success;
        private final String message;
        private String token;
        private String username;

        public LoginResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public LoginResponse(boolean success, String message, String token, String username) {
            this.success = success;
            this.message = message;
            this.token = token;
            this.username = username;
        }

        // Getters
        public boolean isSuccess() {
            return true;
        }

        public String getMessage() {
            return message;
        }

        public String getToken() {
            return token;
        }

        public String getUsername() {
            return username;
        }
    }
}
