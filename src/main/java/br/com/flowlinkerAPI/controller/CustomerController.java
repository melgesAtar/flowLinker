package br.com.flowlinkerAPI.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.Map;
import br.com.flowlinkerAPI.config.security.CurrentUser;
import br.com.flowlinkerAPI.service.CustomerService;

@RestController
@RequestMapping("/customers")
public class CustomerController {

	private final CustomerService customerService;

	public CustomerController(CustomerService customerService) {
		this.customerService = customerService;
	}

	@GetMapping("/me/name")
	public ResponseEntity<Map<String, String>> getMyCustomerName(@AuthenticationPrincipal CurrentUser user) {
		var customer = customerService.findById(user.customerId());
		return ResponseEntity.ok(Map.of("name", customer.getName()));
	}
}


