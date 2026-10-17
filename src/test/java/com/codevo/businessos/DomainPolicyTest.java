package com.codevo.businessos;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import com.codevo.businessos.common.*;
import com.codevo.businessos.billing.invoice.InvoicePolicy;
import com.codevo.businessos.billing.payment.CheckPolicy;
import com.codevo.businessos.billing.subscription.SubscriptionPolicy;
import com.codevo.businessos.security.Permissions;

class DomainPolicyTest {
    private BigDecimal n(String s) { return new BigDecimal(s); }
    @Test void preservesCurrencyPrecisionWithoutSilentRounding() {
        assertThat(Money.amount(n("50.123"),Currency.JOD)).isEqualByComparingTo("50.123");
        assertThatThrownBy(()->Money.amount(n("50.123"),Currency.USD)).isInstanceOf(DomainException.class);
        assertThatThrownBy(()->Money.amount(n("-1"),Currency.ILS)).isInstanceOf(DomainException.class);
    }
    @Test void roundsOnlyExplicitConversionsAndLineTotals() {
        assertThat(Money.rounded(n("1.005"),Currency.USD)).isEqualByComparingTo("1.01");
        assertThat(Money.rounded(n("1.2345"),Currency.JOD)).isEqualByComparingTo("1.235");
    }
    @Test void normalizesRecurringIntervals() {
        assertThat(Money.monthly(n("85"),"MONTHLY",null)).isEqualByComparingTo("85");
        assertThat(Money.monthly(n("1200"),"YEARLY",null)).isEqualByComparingTo("100");
        assertThat(Money.monthly(n("600"),"SEMI_ANNUAL",null)).isEqualByComparingTo("100");
        assertThat(Money.monthly(n("300"),"QUARTERLY",null)).isEqualByComparingTo("100");
        assertThatThrownBy(()->Money.monthly(n("1"),"CUSTOM",null)).isInstanceOf(DomainException.class);
    }
    @Test void computesPartialPaidAndOverdueFromCollectedFunds() {
        LocalDate today=LocalDate.of(2026,10,4);
        assertThat(InvoicePolicy.status(n("100"),n("40"),today.plusDays(1),today)).isEqualTo("PARTIALLY_PAID");
        assertThat(InvoicePolicy.status(n("100"),n("100"),today.minusDays(1),today)).isEqualTo("PAID");
        assertThat(InvoicePolicy.status(n("100"),n("40"),today.minusDays(1),today)).isEqualTo("OVERDUE");
        assertThatThrownBy(()->InvoicePolicy.coverage(n("100"),n("50"),n("51"))).isInstanceOf(DomainException.class);
    }
    @Test void collectedChecksRequireExplicitReversal() {
        CheckPolicy.transition("PENDING","CLEARED");CheckPolicy.transition("DEPOSITED","BOUNCED");
        assertThatThrownBy(()->CheckPolicy.transition("CLEARED","BOUNCED")).isInstanceOf(DomainException.class);
        assertThatThrownBy(()->CheckPolicy.transition("BOUNCED","CLEARED")).isInstanceOf(DomainException.class);
    }
    @Test void renewalHandlesLeapYearAndInclusivePeriods() {
        var monthly=SubscriptionPolicy.renew(LocalDate.of(2028,1,31),"MONTHLY",null);
        assertThat(monthly.start()).isEqualTo(LocalDate.of(2028,2,1));
        assertThat(monthly.end()).isEqualTo(LocalDate.of(2028,2,29));
        assertThat(monthly.renewal()).isEqualTo(LocalDate.of(2028,3,1));
        assertThat(SubscriptionPolicy.renew(LocalDate.of(2026,12,31),"YEARLY",null).end()).isEqualTo(LocalDate.of(2027,12,31));
    }
    @Test void androidDeliverablesAreValidatedWithoutAcceptingGenericArchives() throws Exception {
        var bytes=new java.io.ByteArrayOutputStream();
        try(var zip=new java.util.zip.ZipOutputStream(bytes)) {
            zip.putNextEntry(new java.util.zip.ZipEntry("AndroidManifest.xml"));zip.write("<manifest package='example.test'/>".getBytes());zip.closeEntry();
            zip.putNextEntry(new java.util.zip.ZipEntry("classes.dex"));zip.write(new byte[]{'d','e','x','\n','0','3','5',0});zip.closeEntry();
        }
        assertThat(com.codevo.businessos.file.FileService.type(bytes.toByteArray())).isEqualTo("application/vnd.android.package-archive");
        var generic=new java.io.ByteArrayOutputStream();try(var zip=new java.util.zip.ZipOutputStream(generic)) {zip.putNextEntry(new java.util.zip.ZipEntry("../unsafe.exe"));zip.write(new byte[]{'M','Z'});zip.closeEntry();}
        assertThatThrownBy(()->com.codevo.businessos.file.FileService.type(generic.toByteArray())).isInstanceOf(DomainException.class);
    }
    @Test void rolesDenyUnknownAndSeparateFinanceFromWork() {
        assertThat(Permissions.forRole("UNKNOWN")).isEmpty();
        assertThat(Permissions.forRole("STAFF")).doesNotContain("FINANCE_READ","FINANCE_WRITE","USERS_WRITE");
        assertThat(Permissions.forRole("ACCOUNTANT")).contains("FINANCE_WRITE").doesNotContain("USERS_WRITE","WORK_WRITE");
    }
}
