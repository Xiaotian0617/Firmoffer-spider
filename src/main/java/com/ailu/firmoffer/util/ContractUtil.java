package com.ailu.firmoffer.util;

import org.springframework.context.annotation.Configuration;

import java.text.SimpleDateFormat;
import java.util.Date;

@Configuration
public class ContractUtil {

    /*public static void main(String[] args) {
        String s = judgeContract("181224");
        System.out.print(s);
    }*/

    public String judgeContract(String s) {
        if (StringUtil.isEmpty(s)) {
            return "";
        }
        String balanceDate = s + "160000";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyMMddHHmmss");
        try {
            Date parse = simpleDateFormat.parse(balanceDate);
            long balancetime = parse.getTime();
            Long thisTime = System.currentTimeMillis();
            long l = balancetime - thisTime;
            Double rel = Double.parseDouble(String.valueOf(l)) / 86400000 / 7;
            if (rel <= 1) {
                return "当周合约";
            } else if (rel > 1 && rel <= 2) {
                return "次周合约";
            } else {
                return "季度合约";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

}
