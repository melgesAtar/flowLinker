package br.com.flowlinkerAPI.service;

import br.com.flowlinkerAPI.model.Customer;
import br.com.flowlinkerAPI.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final Logger logger = LoggerFactory.getLogger(CustomerService.class);

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Customer createCustomer(String name, String email, String phoneNumber, String documentType, String documentNumber, String address, String city, String state, String zipCode, Integer offerId){

        Customer newCustomer = new Customer();
        newCustomer.setName(name);
        newCustomer.setEmail(email);
        newCustomer.setPhoneNumber(phoneNumber);
        Customer.DocumentType docType = (documentType.equalsIgnoreCase("cpf")) ? Customer.DocumentType.CPF : Customer.DocumentType.CNPJ;
        newCustomer.setDocumentType(docType);
        newCustomer.setDocumentNumber(documentNumber);
        newCustomer.setAddress(address);
        newCustomer.setCity(city);
        newCustomer.setState(state);
        newCustomer.setZipCode(zipCode);

        switch (offerId) {
            //Basic
            case 471837:
                newCustomer.setOfferType(Customer.OfferType.BASIC);
                break;
            case 472101:
                newCustomer.setOfferType(Customer.OfferType.BASIC);
                break;
            //Standard
            case 471958:
                newCustomer.setOfferType(Customer.OfferType.STANDARD);
                break;
            case 472107:
                newCustomer.setOfferType(Customer.OfferType.STANDARD);
                break;
            //Pro
            case 471962:
                newCustomer.setOfferType(Customer.OfferType.PRO);
                break;
            case 472108:
                newCustomer.setOfferType(Customer.OfferType.PRO);
                break;

            default:
                newCustomer.setOfferType(Customer.OfferType.BASIC);
                break;
        }

        newCustomer.setStatus(Customer.Status.ACTIVE);
        logger.info("Creating customer: " + newCustomer);
        return customerRepository.save(newCustomer);

    }

    //UTILS
    public Customer findCustomerByEmail(String email) {
        return customerRepository.findByEmail(email)
        .orElse(null);
    }
    public Customer updateCustomer(Customer customer) {
        return customerRepository.save(customer);
    }

}
