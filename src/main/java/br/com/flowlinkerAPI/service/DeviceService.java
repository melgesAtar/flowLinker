package br.com.flowlinkerAPI.service;

import br.com.flowlinkerAPI.exceptions.LimitDevicesException;
import org.springframework.stereotype.Service;
import br.com.flowlinkerAPI.repository.DeviceRepository;
import br.com.flowlinkerAPI.model.Device;
import br.com.flowlinkerAPI.model.DeviceStatus;
import org.springframework.transaction.annotation.Transactional;
import br.com.flowlinkerAPI.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.flowlinkerAPI.model.Customer;
import br.com.flowlinkerAPI.model.Customer.OfferType;
import java.util.Map;
import java.util.HashMap;
import br.com.flowlinkerAPI.dto.AddDeviceRequestDTO;
import br.com.flowlinkerAPI.dto.AddDeviceResponseDTO;
import br.com.flowlinkerAPI.exceptions.CustomerNotFoundException;


@Service 
public class DeviceService {
    private final DeviceRepository deviceRepository;
    private final CustomerRepository customerRepository;
    private final Logger logger = LoggerFactory.getLogger(DeviceService.class);
   
    public DeviceService(DeviceRepository deviceRepository, CustomerRepository customerRepository) {
        this.deviceRepository = deviceRepository;
        this.customerRepository = customerRepository;
    }

    private static final Map<Customer.OfferType, Integer> MAX_DEVICES = new HashMap<>();
    static {
        MAX_DEVICES.put(OfferType.BASIC, 2);
        MAX_DEVICES.put(OfferType.STANDARD, 5);
        MAX_DEVICES.put(OfferType.PRO, 10);
    }

    @Transactional
    public AddDeviceResponseDTO addDevice(AddDeviceRequestDTO addDeviceRequestDTO){
        
        Customer customer = customerRepository.findById(addDeviceRequestDTO.getCustomerId())
            .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));
    
        int currentCount = deviceRepository.countByCustomerIdAndStatus(addDeviceRequestDTO.getCustomerId(), DeviceStatus.ACTIVE);
        int max = MAX_DEVICES.getOrDefault(customer.getOfferType(), 0);
    
        if (currentCount >= max) {
           throw new LimitDevicesException("Limit of devices by employee reached " + customer.getOfferType() + " (máx: " + max + ")");
        }

        logger.info("Device adicionado para customer {} (plano: {}, devices atuais: {})", customer.getEmail(), customer.getOfferType(), currentCount + 1);
        
        Device newDevice = new Device();
        newDevice.setFingerprint(addDeviceRequestDTO.getFingerprint());
        newDevice.setName(addDeviceRequestDTO.getName());
        newDevice.setCustomer(customer);
        newDevice.setStatus(DeviceStatus.ACTIVE);
        deviceRepository.save(newDevice);

        return new AddDeviceResponseDTO("Device added successfully for customer " + customer.getEmail() + "Device: " + newDevice.getFingerprint());
    }

    public void deactivateByCustomerId(Long customerId) {
        var devices = deviceRepository.findByCustomerId(customerId);
        for (Device d : devices) {
            d.setStatus(DeviceStatus.INACTIVE);
        }
        deviceRepository.saveAll(devices);
    }

    public void saveDevice(Device device) {
        deviceRepository.save(device);
    }
}
