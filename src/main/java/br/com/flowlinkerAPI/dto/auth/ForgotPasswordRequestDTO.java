package br.com.flowlinkerAPI.dto.auth;

public class ForgotPasswordRequestDTO {
	private String email;
	// opcional: permite o front informar a base do link de reset
	private String redirectBaseUrl;

	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getRedirectBaseUrl() {
		return redirectBaseUrl;
	}
	public void setRedirectBaseUrl(String redirectBaseUrl) {
		this.redirectBaseUrl = redirectBaseUrl;
	}
}


