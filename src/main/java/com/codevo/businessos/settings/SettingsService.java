package com.codevo.businessos.settings;
import java.util.*;
import java.time.ZoneId;
import jakarta.validation.constraints.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import com.codevo.businessos.common.*;
import com.codevo.businessos.common.Currency;
import com.codevo.businessos.activity.ActivityService;
@Service
public class SettingsService {
    public record Request(@NotBlank @Size(max=255) String companyName,@Size(max=2048) @Pattern(regexp="^(https?://.*)?$") String logoUrl,
        @NotNull Currency defaultCurrency,@NotNull Currency reportingCurrency,@NotBlank @Size(max=80) String timezone,
        @NotBlank @Pattern(regexp="[A-Z0-9-]{1,20}") String invoicePrefix,
        @Min(0) @Max(90) int paymentWarningDays,@Min(0) @Max(90) int subscriptionWarningDays,@Min(0) @Max(90) int deadlineWarningDays,long version) {}
    private final JdbcTemplate jdbc;private final ActivityService activity;
    public SettingsService(JdbcTemplate jdbc,ActivityService activity) { this.jdbc=jdbc;this.activity=activity; }
    @PreAuthorize("isAuthenticated()")
    public Map<String,Object> get() { return jdbc.queryForMap("select * from organization_settings where id=1"); }
    @Transactional @PreAuthorize("hasAuthority('SETTINGS_WRITE')")
    public Map<String,Object> update(Request r) {
        try { ZoneId.of(r.timezone()); } catch(Exception e) { throw DomainException.invalid("Invalid timezone"); }
        int changed=jdbc.update("update organization_settings set company_name=?,logo_url=?,default_currency=?,reporting_currency=?,timezone=?,invoice_prefix=?,payment_warning_days=?,subscription_warning_days=?,deadline_warning_days=?,version=version+1 where id=1 and version=?",
            r.companyName(),r.logoUrl(),r.defaultCurrency().name(),r.reportingCurrency().name(),r.timezone(),r.invoicePrefix(),r.paymentWarningDays(),r.subscriptionWarningDays(),r.deadlineWarningDays(),r.version());
        if(changed!=1) throw DomainException.conflict("Settings changed; refresh and retry");
        activity.record("settings",new UUID(0,1),null,"UPDATED","Organization settings updated");return get();
    }
}
