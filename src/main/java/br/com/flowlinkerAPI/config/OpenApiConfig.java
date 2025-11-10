package br.com.flowlinkerAPI.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI flowlinkerOpenAPI() {
		return new OpenAPI()
			.info(new Info()
				.title("FlowLinker API")
				.version("v1")
				.description("Documentação da API FlowLinker (login, sessão e redefinição de senha).")
				.contact(new Contact().name("FlowLinker").email("suporte@flowlinker.com.br")))
			.servers(List.of(
				new Server().url("/").description("Servidor atual")
			));
	}
}


