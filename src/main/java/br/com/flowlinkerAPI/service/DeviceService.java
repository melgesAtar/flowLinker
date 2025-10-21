package br.com.flowlinkerAPI.service;

import org.springframework.stereotype.Service;
import br.com.flowlinkerAPI.repository.DeviceRepository;
import br.com.flowlinkerAPI.model.Device;
import org.springframework.transaction.annotation.Transactional;
import br.com.flowlinkerAPI.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.flowlinkerAPI.model.Customer;
import java.util.Map;
import java.util.HashMap;
import dto.AddDeviceRequestDTO;
import dto.AddDeviceResponseDTO;
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
        MAX_DEVICES.put(Customer.OfferType.BASIC, 3);
        MAX_DEVICES.put(Customer.OfferType.STANDARD, 5);
        MAX_DEVICES.put(Customer.OfferType.PRO, 10);
    }

    @Transactional
    public AddDeviceResponseDTO addDevice(AddDeviceRequestDTO addDeviceRequestDTO) throws CustomerNotFoundException {
        
        Customer customer = customerRepository.findById(addDeviceRequestDTO.getCustomerId())
            .orElseThrow(() -> new CustomerNotFoundException("Customer não encontrado"));
    
        int currentCount = deviceRepository.countByCustomerId(addDeviceRequestDTO.getCustomerId());
        int max = MAX_DEVICES.getOrDefault(customer.getOfferType(), 0);
    
        if (currentCount >= max) {
            throw new RuntimeException("Limite de devices excedido para plano " + customer.getOfferType() + " (máx: " + max + ")");
        }

        logger.info("Device adicionado para customer {} (plano: {}, devices atuais: {})", customer.getEmail(), customer.getOfferType(), currentCount + 1);
        
        Device newDevice = new Device();
        newDevice.setFingerprint(addDeviceRequestDTO.getFingerprint());
        newDevice.setName(addDeviceRequestDTO.getName());
        newDevice.setCustomer(customer);
        deviceRepository.save(newDevice);

        return new AddDeviceResponseDTO("Device added successfully for customer " + customer.getEmail() + "Device: " + newDevice.getFingerprint());
    }

    public void deleteByCustomerId(Long customerId) {
        deviceRepository.deleteByCustomerId(customerId);
    }

    public void saveDevice(Device device) {
        deviceRepository.save(device);
    }
}
