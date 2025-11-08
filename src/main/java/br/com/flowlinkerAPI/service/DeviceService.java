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
import br.com.flowlinkerAPI.dto.AddDeviceRequestDTO;
import br.com.flowlinkerAPI.dto.AddDeviceResponseDTO;
import br.com.flowlinkerAPI.exceptions.CustomerNotFoundException;


@Service 
public class DeviceService {
    private final DeviceRepository deviceRepository;
    private final CustomerRepository customerRepository;
    private final Logger logger = LoggerFactory.getLogger(DeviceService.class);
    private final DevicePolicyService devicePolicyService;
   
    public DeviceService(DeviceRepository deviceRepository, CustomerRepository customerRepository, DevicePolicyService devicePolicyService) {
        this.deviceRepository = deviceRepository;
        this.customerRepository = customerRepository;
        this.devicePolicyService = devicePolicyService;
    }

    @Transactional
    public AddDeviceResponseDTO addDevice(AddDeviceRequestDTO addDeviceRequestDTO){
        
        Customer customer = customerRepository.findById(addDeviceRequestDTO.getCustomerId())
            .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));
    
        int currentCount = deviceRepository.countByCustomerIdAndStatus(addDeviceRequestDTO.getCustomerId(), DeviceStatus.ACTIVE);
        int max = devicePolicyService.getMaxDevices(customer.getOfferType());
    
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
