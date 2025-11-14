package br.com.flowlinkerAPI.service;

import br.com.flowlinkerAPI.dto.AddDeviceRequestDTO;
import br.com.flowlinkerAPI.dto.AddDeviceResponseDTO;
import br.com.flowlinkerAPI.dto.device.DeviceCountsDTO;
import br.com.flowlinkerAPI.dto.device.DeviceSummaryDTO;
import br.com.flowlinkerAPI.exceptions.CustomerNotFoundException;
import br.com.flowlinkerAPI.exceptions.LimitDevicesException;
import br.com.flowlinkerAPI.model.Customer;
import br.com.flowlinkerAPI.model.Device;
import br.com.flowlinkerAPI.model.DeviceStatus;
import br.com.flowlinkerAPI.repository.CustomerRepository;
import br.com.flowlinkerAPI.repository.DeviceRepository;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


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

        Long customerId = Objects.requireNonNull(addDeviceRequestDTO.getCustomerId(), "customerId");
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));
    
        int currentCount = deviceRepository.countByCustomerIdAndStatus(customerId, DeviceStatus.ACTIVE);
        int max = devicePolicyService.getAllowedDevices(customerId, customer.getOfferType());
    
        if (currentCount >= max) {
           throw new LimitDevicesException("Limit of devices by employee reached " + customer.getOfferType() + " (máx: " + max + ")");
        }

        logger.info("Device adicionado para customer {} (plano: {}, devices atuais: {})", customer.getEmail(), customer.getOfferType(), currentCount + 1);
        
        Device newDevice = new Device();
        newDevice.setFingerprint(addDeviceRequestDTO.getFingerprint());
        newDevice.setName(addDeviceRequestDTO.getName());
        newDevice.setCustomer(customer);
        newDevice.setStatus(DeviceStatus.ACTIVE);
        deviceRepository.save(Objects.requireNonNull(newDevice));

        return new AddDeviceResponseDTO("Device added successfully for customer " + customer.getEmail() + "Device: " + newDevice.getFingerprint());
    }

    public void deactivateByCustomerId(Long customerId) {
        customerId = Objects.requireNonNull(customerId, "customerId");
        List<Device> devices = deviceRepository.findByCustomerId(customerId);
        for (Device d : devices) {
            d.setStatus(DeviceStatus.INACTIVE);
        }
        if (!devices.isEmpty()) {
            deviceRepository.saveAll(devices);
        }
    }

    public void saveDevice(Device device) {
        deviceRepository.save(Objects.requireNonNull(device));
    }

    @Transactional(readOnly = true)
    public List<DeviceSummaryDTO> listDevices(Long customerId, DeviceStatus status) {
        customerId = Objects.requireNonNull(customerId, "customerId");
        List<Device> devices = (status != null)
                ? deviceRepository.findByCustomerIdAndStatus(customerId, status)
                : deviceRepository.findByCustomerId(customerId);
        return devices.stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public DeviceCountsDTO getCounts(Long customerId) {
        customerId = Objects.requireNonNull(customerId, "customerId");
        int total = deviceRepository.countByCustomerId(customerId);
        int active = deviceRepository.countByCustomerIdAndStatus(customerId, DeviceStatus.ACTIVE);
        int inactive = deviceRepository.countByCustomerIdAndStatus(customerId, DeviceStatus.INACTIVE);
        int allowed = devicePolicyService.getAllowedDevices(customerId,
                customerRepository.findById(customerId).map(Customer::getOfferType).orElse(null));
        return new DeviceCountsDTO(customerId, total, active, inactive, allowed);
    }

    @Transactional
    public DeviceSummaryDTO updateStatus(Long customerId, Long deviceId, DeviceStatus newStatus) {
        customerId = Objects.requireNonNull(customerId, "customerId");
        deviceId = Objects.requireNonNull(deviceId, "deviceId");
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new CustomerNotFoundException("Device not found"));
        Customer owner = device.getCustomer();
        Long ownerId = owner != null ? owner.getId() : null;
        if (ownerId == null) {
            throw new CustomerNotFoundException("Device does not belong to this customer");
        }
        if (!ownerId.equals(customerId)) {
            throw new CustomerNotFoundException("Device does not belong to this customer");
        }
        device.setStatus(newStatus);
        deviceRepository.save(Objects.requireNonNull(device));
        return toSummary(device);
    }

    private DeviceSummaryDTO toSummary(Device device) {
        return new DeviceSummaryDTO(
                device.getId(),
                device.getName(),
                device.getFingerprint(),
                device.getDeviceId(),
                device.getStatus(),
                device.getOsName(),
                device.getOsVersion(),
                device.getAppVersion(),
                device.getLastIp(),
                device.getLastSeenAt(),
                device.getCreatedAt()
        );
    }
}
