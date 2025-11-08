package br.com.flowlinkerAPI.service;

import br.com.flowlinkerAPI.model.Customer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DevicePolicyService {

    @Value("${devices.limits.basic:2}")
    private int limitBasic;

    @Value("${devices.limits.standard:5}")
    private int limitStandard;

    @Value("${devices.limits.pro:10}")
    private int limitPro;

    public int getMaxDevices(Customer.OfferType offerType) {
        if (offerType == null) return 0;
        switch (offerType) {
            case BASIC:
                return limitBasic;
            case STANDARD:
                return limitStandard;
            case PRO:
                return limitPro;
            default:
                return 0;
        }
    }
}


