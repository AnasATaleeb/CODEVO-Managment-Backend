package com.codevo.businessos.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info=@Info(title="CODEVO Business OS",version="1.0",description="Authenticated business operations API. Decimal money, independent checks, explicit audit history."),security=@SecurityRequirement(name="bearerAuth"))
@SecurityScheme(name="bearerAuth",type=SecuritySchemeType.HTTP,scheme="bearer",bearerFormat="JWT")
public class OpenApiConfig {}
