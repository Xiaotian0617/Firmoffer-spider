package com.ailu.firmoffer.exchange.signature;

import com.ailu.firmoffer.util.HmacUtil;

import java.util.Map;

/**
 * @Description:
 * @author: liu zhenming
 * @version: V1.0
 * @date: 2018/5/4 10:33
 */
public class BiboxSignature {

    /**
     * @param secret
     * @param cmds
     * @return
     */
    public String getSignString(String secret, String cmds) throws Exception {
        return HmacUtil.byteArrayToHexString(HmacUtil.encryptHMAC(cmds.getBytes(), secret));
    }

    /**
     * @param cmd
     * @param params
     * @return
     */
    public String getCmdsString(String cmd, Map<String, Object> params) {
        StringBuilder sb = new StringBuilder();
        sb.append("[{");
        sb.append("\"cmd\"");
        sb.append(":");
        sb.append("\"" + cmd + "\",");
        sb.append("\"body\":{");
        params.forEach((k, v) -> {
            sb.append("\"" + k + "\":");
            sb.append(v);
            sb.append(",");
        });
        if (sb.lastIndexOf(",") != 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append("}");
        sb.append("}]");
        return sb.toString();
    }

}
