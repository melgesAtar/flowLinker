package br.com.flowlinkerAPI.controller;

import br.com.flowlinkerAPI.exceptions.LimitDevicesException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import br.com.flowlinkerAPI.service.DeviceService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import br.com.flowlinkerAPI.dto.AddDeviceRequestDTO;
import br.com.flowlinkerAPI.dto.AddDeviceResponseDTO;


@RestController
@RequestMapping("/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping("/add")
    public ResponseEntity<AddDeviceResponseDTO> addDevice(@RequestBody AddDeviceRequestDTO addDeviceRequestDTO) throws LimitDevicesException {
        AddDeviceResponseDTO addDeviceResponseDTO = deviceService.addDevice(addDeviceRequestDTO);
        return ResponseEntity.ok(new AddDeviceResponseDTO(addDeviceResponseDTO.getMessage()));
    }

}

