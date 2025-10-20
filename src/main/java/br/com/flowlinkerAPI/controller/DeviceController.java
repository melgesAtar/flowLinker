package br.com.flowlinkerAPI.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import br.com.flowlinkerAPI.service.DeviceService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import dto.AddDeviceRequestDTO;
import dto.AddDeviceResponseDTO;


@RestController
@RequestMapping("/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping
    public ResponseEntity<AddDeviceResponseDTO> addDevice(@RequestBody AddDeviceRequestDTO addDeviceRequestDTO) {
        return ResponseEntity.ok(deviceService.addDevice(addDeviceRequestDTO));
    }

}

