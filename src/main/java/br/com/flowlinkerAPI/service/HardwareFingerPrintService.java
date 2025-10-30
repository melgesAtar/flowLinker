package br.com.flowlinkerAPI.service;

import org.springframework.stereotype.Service;

@Service
public class HardwareFingerPrintService {
    
    public double diffRatio(String a, String b) {
        if(a == null || b == null) return 1.0;
        int len = Math.min(a.length(), b.length());
        if(len == 0) return 1.0;
        int diff = 0;
        for(int i = 0; i < len; i++) {
            if(a.charAt(i) != b.charAt(i)) {
                diff++;
            }
        }
        diff += Math.abs(a.length() - b.length());
        return (double) diff / (double) Math.max(a.length(), b.length());
    }
}
