package com.codevo.businessos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import tools.jackson.databind.*;
import com.codevo.businessos.common.BusinessDates;

/** Real MySQL exercises migrations, allocation caches, source snapshots, and row-lock idempotency. */
@SpringBootTest(properties={"app.seed=false","app.secure-cookies=false","app.bootstrap-owner-email=acceptance-owner@example.test",
    "app.bootstrap-owner-password=Test-only-owner-password-2026","app.jwt-secret=test-only-jwt-key-with-more-than-thirty-two-bytes",
    "app.jobs.initial-delay-ms=3600000","springdoc.api-docs.enabled=false","springdoc.swagger-ui.enabled=false"})
@AutoConfigureMockMvc
class FinanceWorkflowTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired BusinessDates dates;
    String actor;
    @DynamicPropertySource static void database(DynamicPropertyRegistry p) {
        TestDatabase.configure(p);
    }
    @BeforeEach void actor() { actor=jdbc.queryForObject("select id from app_users where email='acceptance-owner@example.test'",String.class); }
    Map<String,Object> obj(Object... entries) { Map<String,Object> out=new LinkedHashMap<>();for(int i=0;i<entries.length;i+=2)out.put((String)entries[i],entries[i+1]);return out; }
    JsonNode request(String method,String path,Object body,int expected) throws Exception {
        var request=org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(org.springframework.http.HttpMethod.valueOf(method),"/api/v1"+path.split("\\?",2)[0])
            .with(user(actor).authorities(List.of(new SimpleGrantedAuthority("FINANCE_READ"),new SimpleGrantedAuthority("FINANCE_WRITE"),new SimpleGrantedAuthority("CRM_READ"),new SimpleGrantedAuthority("CRM_WRITE"),new SimpleGrantedAuthority("WORK_READ"),new SimpleGrantedAuthority("WORK_WRITE"),new SimpleGrantedAuthority("CATALOG_WRITE"))));
        if(path.contains("?"))for(String pair:path.substring(path.indexOf('?')+1).split("&")){var kv=pair.split("=",2);request.queryParam(kv[0],java.net.URLDecoder.decode(kv.length>1?kv[1]:"",java.nio.charset.StandardCharsets.UTF_8));}
        if(body!=null)request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        var response=mvc.perform(request).andReturn().getResponse();assertThat(response.getStatus()).as(method+" "+path).isEqualTo(expected);
        return response.getContentAsString().isBlank()?json.readTree("null"):json.readTree(response.getContentAsString());
    }
    String client() throws Exception { return request("POST","/clients",obj("displayName","Finance workflow "+UUID.randomUUID(),"status","ACTIVE","preferredCurrency","ILS"),201).get("id").asText(); }
    String project(String client) throws Exception { return request("POST","/projects",obj("clientId",client,"name","Finance project "+UUID.randomUUID(),"projectCode","FIN-"+UUID.randomUUID(),"status","IN_PROGRESS","priority","NORMAL","currency","ILS","estimatedPrice","0","estimatedCost","0"),201).get("id").asText(); }
    String service(String project,String name,String amount,String billing) throws Exception {
        String catalog=request("POST","/services",obj("name",name,"code","FIN-"+UUID.randomUUID(),"category","Testing","defaultBillingType",billing,"defaultPrice",amount,"defaultCost","0","defaultCurrency","ILS","active",true),201).get("id").asText();
        return request("POST","/project-services",obj("projectId",project,"serviceCatalogItemId",catalog,"customName",name,"billingType",billing,"billingInterval",billing.equals("ONE_TIME")?null:billing,"price",amount,"cost","0","currency","ILS","quantity",1,"status","DELIVERED","recurring",!billing.equals("ONE_TIME")),201).get("id").asText();
    }
    JsonNode invoice(String c,String p,String s,String amount,String currency) throws Exception {
        return request("POST","/invoices",obj("clientId",c,"projectId",p,"status","ISSUED","issueDate",dates.today().minusDays(2),"dueDate",dates.today().minusDays(1),"currency",currency,"discount","0","tax","0","items",List.of(obj("projectServiceId",s,"description","Finance service","quantity",1,"unitPrice",amount))),201);
    }
    Map<String,Object> check(String amount) { return obj("method","CHECK","amount",amount,"currency","ILS","check",obj("checkNumber","FIN-"+UUID.randomUUID(),"bankName","Finance test bank","issuerName","Synthetic issuer","receivedDate",dates.today(),"dueDate",dates.today().plusDays(2))); }
    JsonNode payment(String c,String invoice,List<Map<String,Object>> parts) throws Exception {
        return request("POST","/payments",obj("clientId",c,"invoiceId",invoice,"paymentDate",dates.today(),"parts",parts),201);
    }
    JsonNode receivable(String c) throws Exception { return request("GET","/receivables?clientId="+c,null,200).get("content").get(0); }
    void money(JsonNode node,String field,String amount) { assertThat(node.get(field).decimalValue()).as(field).isEqualByComparingTo(amount); }

    @Test void compoundCollectionClearingBounceAndReplacementPreserveTheReceivable() throws Exception {
        String c=client(),p=project(c),s=service(p,"Development","5000","ONE_TIME");String invoice=invoice(c,p,s,"5000","ILS").get("id").asText();
        var payment=payment(c,invoice,List.of(obj("method","BANK_TRANSFER","amount","2000","currency","ILS","bankName","Finance test bank"),obj("method","CASH","amount","1000","currency","ILS"),check("1000"),check("1000")));
        var r=receivable(c);money(r,"collectedAmount","3000");money(r,"pendingCheckCoverage","2000");money(r,"outstandingAmount","2000");money(r,"uncoveredOutstanding","0");
        assertThat(r.get("coverageStatus").asText()).isEqualTo("COVERED_BY_CHECKS");assertThat(r.get("nextAction").asText()).isEqualTo("COLLECT_CHECKS");
        List<JsonNode> recordedChecks=new ArrayList<>();for(var part:payment.get("parts"))if(!part.get("check").isNull())recordedChecks.add(part.get("check"));
        assertThat(recordedChecks).hasSize(2);var first=recordedChecks.get(0);var second=recordedChecks.get(1);
        request("POST","/checks/"+first.get("id").asText()+"/status",obj("status","CLEARED","date",dates.today(),"version",first.get("version").asLong()),200);
        request("POST","/checks/"+second.get("id").asText()+"/status",obj("status","BOUNCED","date",dates.today(),"version",second.get("version").asLong(),"reason","Synthetic rejection"),200);
        r=receivable(c);money(r,"collectedAmount","4000");money(r,"pendingCheckCoverage","0");money(r,"outstandingAmount","1000");money(r,"uncoveredOutstanding","1000");
        assertThat(r.get("hasBouncedCheck").asBoolean()).isTrue();assertThat(r.get("nextAction").asText()).isEqualTo("RECORD_PAYMENT");
        var checks=request("GET","/checks?projectId="+p+"&currency=ILS&bankName=Finance%20test%20bank",null,200);assertThat(checks.get("totalElements").asInt()).isEqualTo(2);
        var checkRow=checks.get("content").get(0);assertThat(checkRow.get("project_id").asText()).isEqualTo(p);money(checkRow,"invoice_uncovered_outstanding","1000");
        var summary=request("GET","/projects/"+p+"/financial-summary",null,200).get("currencies").get(0);money(summary,"contracted","5000");money(summary,"collected","4000");money(summary,"uncoveredOutstanding","1000");money(summary,"cashProfit","4000");
        assertThat(request("GET","/financial-timeline?invoiceId="+invoice,null,200).get("totalElements").asInt()).isGreaterThan(5);
        payment(c,invoice,List.of(obj("method","BANK_TRANSFER","amount","1000","currency","ILS","bankName","Finance test bank")));
        assertThat(request("GET","/receivables?clientId="+c,null,200).get("totalElements").asInt()).isZero();
        money(request("GET","/receivables?clientId="+c+"&status=PAID",null,200).get("content").get(0),"collectedAmount","5000");
        var attention=request("GET","/dashboard/attention",null,200);
        for(var item:attention) if(item.get("type").asText().equals("CHECK_BOUNCED")) assertThat(item.get("id").asText()).isNotEqualTo(checkRow.get("id").asText());
    }
    @Test void currencyFiltersTotalsAndPendingCoverageNeverMixCurrencies() throws Exception {
        String c=client();invoice(c,null,null,"900","ILS");invoice(c,null,null,"125","USD");
        var all=request("GET","/receivables?clientId="+c+"&size=1",null,200);assertThat(all.get("content").size()).isEqualTo(1);assertThat(all.get("totalElements").asInt()).isEqualTo(2);assertThat(all.get("totals").size()).isEqualTo(2);
        var usd=request("GET","/receivables?clientId="+c+"&currency=USD&status=OVERDUE",null,200);assertThat(usd.get("totalElements").asInt()).isEqualTo(1);money(usd.get("totals").get(0),"outstandingAmount","125");
        request("GET","/receivables?clientId="+c+"&sort=total%20desc",null,400);
        request("GET","/receivables?clientId="+c+"&status=INVALID",null,400);
    }
    @Test void sourceSubscriptionAndConcurrentRenewalInvoiceReuseTermsWithoutAdvancingThePeriod() throws Exception {
        String c=client(),p=project(c),s=service(p,"Yearly hosting","300","YEARLY");
        invoice(c,p,s,"300","ILS");
        var subscription=request("POST","/subscriptions/from-service",obj("projectServiceId",s,"startDate",dates.today()),201);
        money(subscription,"amount","300");assertThat(subscription.get("clientId").asText()).isEqualTo(c);assertThat(subscription.get("billingInterval").asText()).isEqualTo("YEARLY");
        request("POST","/subscriptions/from-service",obj("projectServiceId",s),409);
        String id=subscription.get("id").asText();Map<String,Object> command=obj("version",subscription.get("version").asLong(),"issueDate",dates.today(),"dueDate",dates.today().plusDays(7));
        var barrier=new CyclicBarrier(2);ExecutorService pool=Executors.newFixedThreadPool(2);
        try {
            Callable<String> generate=()->{barrier.await(5,TimeUnit.SECONDS);return request("POST","/subscriptions/"+id+"/renewal-invoice",command,200).get("id").asText();};
            var one=pool.submit(generate);var two=pool.submit(generate);assertThat(one.get(20,TimeUnit.SECONDS)).isEqualTo(two.get(20,TimeUnit.SECONDS));
        } finally { pool.shutdownNow(); }
        assertThat(jdbc.queryForObject("select count(*) from subscription_renewal_invoices where subscription_id=?",Integer.class,id)).isEqualTo(1);
        var after=request("GET","/subscriptions/"+id,null,200);assertThat(after.get("currentPeriodEnd")).isEqualTo(subscription.get("currentPeriodEnd"));
        var summary=request("GET","/projects/"+p+"/financial-summary",null,200).get("currencies").get(0);money(summary,"contracted","300");money(summary,"remainingToInvoice","0");money(summary,"invoiced","600");money(summary,"mrr","25");
        var renewals=request("GET","/subscriptions/renewals?clientId="+c,null,200);assertThat(renewals.get("content").get(0).get("invoiceId").isNull()).isFalse();assertThat(renewals.get("totals").get(0).get("unpaidRenewalInvoices").asInt()).isEqualTo(1);
        assertThat(renewals.get("content").get(0).get("renewalDate").asText()).isEqualTo(subscription.get("renewalDate").asText());
        assertThat(renewals.get("content").get(0).get("currentPeriodEnd").asText()).isEqualTo(subscription.get("currentPeriodEnd").asText());
        String firstInvoice=renewals.get("content").get(0).get("invoiceId").asText();
        request("POST","/invoices/"+firstInvoice+"/void",obj("reason","Correct synthetic billing date"),200);
        var reissued=request("POST","/subscriptions/"+id+"/renewal-invoice",command,200);
        assertThat(reissued.get("id").asText()).isNotEqualTo(firstInvoice);
        assertThat(jdbc.queryForObject("select count(*) from subscription_renewal_invoices where subscription_id=?",Integer.class,id)).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from subscription_renewal_invoices where subscription_id=? and retired_at is null",Integer.class,id)).isEqualTo(1);
        assertThat(request("GET","/subscriptions/renewals?clientId="+c,null,200).get("totalElements").asInt()).isEqualTo(1);
        var advanced=request("POST","/subscriptions/"+id+"/renew",obj("version",after.get("version").asLong(),"reason","Start next synthetic service period"),200);
        request("POST","/subscriptions/"+id+"/renewal-invoice",obj("version",advanced.get("version").asLong(),"issueDate",dates.today(),"dueDate",dates.today().plusDays(7)),200);
        var focused=request("GET","/subscriptions/renewals?subscriptionId="+id,null,200);assertThat(focused.get("totalElements").asInt()).isEqualTo(1);assertThat(focused.get("totals").get(0).get("unpaidRenewalInvoices").asInt()).isEqualTo(2);
    }
    @Test void financeReadModelsRejectAnonymousAndStaff() throws Exception {
        for(String path:List.of("/receivables","/subscriptions/renewals","/financial-timeline")) {
            assertThat(mvc.perform(get("/api/v1"+path)).andReturn().getResponse().getStatus()).isEqualTo(401);
            assertThat(mvc.perform(get("/api/v1"+path).with(user(actor).authorities(new SimpleGrantedAuthority("WORK_READ"),new SimpleGrantedAuthority("CRM_READ")))).andReturn().getResponse().getStatus()).isEqualTo(403);
        }
    }
    @Autowired com.codevo.businessos.billing.subscription.SubscriptionService subscriptionService;
    @Autowired com.codevo.businessos.billing.subscription.SubscriptionInvoiceAutomation automation;
    @Test void milestoneScheduleUsesAgreementsAndGeneratesOneInvoiceWithoutCollectingMoney() throws Exception {
        String c=client(),p=project(c),service=service(p,"Milestone platform","9000","ONE_TIME");
        List<JsonNode> milestones=new ArrayList<>();
        for(String percent:List.of("30","40","30"))milestones.add(request("POST","/projects/"+p+"/billing-plan",obj("name","Installment "+percent,"currency","ILS","dueDate",dates.today().plusDays(10),"services",List.of(obj("projectServiceId",service,"percentage",percent))),201));
        money(milestones.get(0),"amount","2700");money(milestones.get(1),"amount","3600");
        var summary=request("GET","/projects/"+p+"/financial-summary",null,200).get("currencies").get(0);money(summary,"contracted","9000");money(summary,"invoiced","0");money(summary,"collected","0");
        request("POST","/projects/"+p+"/billing-plan",obj("name","Overplanned","currency","ILS","dueDate",dates.today(),"services",List.of(obj("projectServiceId",service,"amount","1"))),409);
        assertThat(request("GET","/projects/"+p+"/billing-plan",null,200).size()).isEqualTo(3);
        var first=milestones.get(0);var command=obj("version",first.get("version").asLong(),"issueDate",dates.today(),"status","ISSUED");
        var barrier=new CyclicBarrier(2);var pool=Executors.newFixedThreadPool(2);String invoiceId;
        try {
            Callable<String> generate=()->{barrier.await(5,TimeUnit.SECONDS);return request("POST","/projects/"+p+"/billing-plan/"+first.get("id").asText()+"/invoice",command,200).get("id").asText();};
            var one=pool.submit(generate);var two=pool.submit(generate);invoiceId=one.get(20,TimeUnit.SECONDS);assertThat(two.get(20,TimeUnit.SECONDS)).isEqualTo(invoiceId);
        } finally { pool.shutdownNow(); }
        money(request("GET","/invoices/"+invoiceId,null,200),"total","2700");money(request("GET","/invoices/"+invoiceId,null,200),"paidAmount","0");
        request("POST","/projects/"+p+"/billing-plan/"+first.get("id").asText()+"/cancel",obj("version",0,"reason","Synthetic cancellation"),409);
        request("POST","/invoices/"+invoiceId+"/void",obj("reason","Correct synthetic installment"),200);
        var replacement=request("POST","/projects/"+p+"/billing-plan/"+first.get("id").asText()+"/invoice",command,200);assertThat(replacement.get("id").asText()).isNotEqualTo(invoiceId);
        assertThat(jdbc.queryForObject("select count(*) from project_billing_milestone_invoices where milestone_id=?",Integer.class,first.get("id").asText())).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from payments where client_id=?",Integer.class,c)).isZero();
    }
    @Test void projectCloseoutAllowsUnpaidInvoicesAndKeepsStaffFinancialDataHidden() throws Exception {
        String c=client(),p=project(c),service=service(p,"Closeout delivery","900","ONE_TIME");var invoice=invoice(c,p,service,"900","ILS");
        request("POST","/tasks",obj("clientId",c,"projectId",p,"title","Final handoff","status","TODO","priority","NORMAL","sortOrder",0),201);
        var review=request("GET","/projects/"+p+"/closeout",null,200);assertThat(review.get("totalTasks").asInt()).isEqualTo(1);money(review.get("financial").get("currencies").get(0),"outstandingAmount","900");
        var staff=user(actor).authorities(new SimpleGrantedAuthority("WORK_READ"),new SimpleGrantedAuthority("WORK_WRITE"),new SimpleGrantedAuthority("CRM_READ"));
        var response=mvc.perform(get("/api/v1/projects/"+p+"/closeout").with(staff)).andReturn().getResponse();assertThat(response.getStatus()).isEqualTo(200);assertThat(json.readTree(response.getContentAsString()).get("financial").isNull()).isTrue();
        request("POST","/projects/"+p+"/closeout",obj("version",review.get("version").asLong(),"reason","Reviewed delivery risk","reviewed",false),400);
        var complete=request("POST","/projects/"+p+"/closeout",obj("version",review.get("version").asLong(),"reason","Client accepted delivery; collect balance later","reviewed",true),200);
        assertThat(complete.get("status").asText()).isEqualTo("COMPLETED");money(request("GET","/invoices/"+invoice.get("id").asText(),null,200),"balanceDue","900");
        assertThat(jdbc.queryForObject("select open_tasks from project_closeouts where project_id=?",Long.class,p)).isEqualTo(1);
        request("POST","/projects/"+p+"/closeout",obj("version",review.get("version").asLong(),"reason","Stale closeout retry","reviewed",true),409);
    }
    @Test void explicitClientMergePreservesOriginalInvoiceIdentityAndRequiresAnUnchangedPreview() throws Exception {
        String source=client(),target=client(),p=project(source),service=service(p,"Merge service","1000","ONE_TIME");
        var sourceBefore=request("GET","/clients/"+source,null,200);var invoice=invoice(source,p,service,"1000","ILS");String invoiceId=invoice.get("id").asText();
        var paid=payment(source,invoiceId,List.of(obj("method","CASH","amount","400","currency","ILS"),check("600")));String paymentId=paid.get("id").asText();paid=request("GET","/payments/"+paymentId,null,200);
        var preview=request("GET","/clients/"+source+"/merge-preview?targetId="+target,null,200);assertThat(preview.get("records").get("projects").asInt()).isEqualTo(1);
        request("POST","/notes",obj("clientId",source,"body","Added after preview"),201);
        var stale=obj("targetId",target,"sourceVersion",preview.get("sourceVersion").asLong(),"targetVersion",preview.get("targetVersion").asLong(),"fingerprint",preview.get("fingerprint").asText(),"reason","Verified duplicate contact","confirmed",true);
        request("POST","/clients/"+source+"/merge",stale,409);assertThat(jdbc.queryForObject("select client_id from projects where id=?",String.class,p)).isEqualTo(source);
        preview=request("GET","/clients/"+source+"/merge-preview?targetId="+target,null,200);stale.put("fingerprint",preview.get("fingerprint").asText());
        request("POST","/clients/"+source+"/merge",stale,200);
        assertThat(request("GET","/clients/"+source,null,200).get("mergedIntoId").asText()).isEqualTo(target);
        assertThat(jdbc.queryForObject("select client_id from projects where id=?",String.class,p)).isEqualTo(target);
        var after=request("GET","/invoices/"+invoiceId,null,200);assertThat(after.get("clientId").asText()).isEqualTo(target);assertThat(after.get("originalClientId").asText()).isEqualTo(source);assertThat(after.get("billingClientName").asText()).isEqualTo(sourceBefore.get("displayName").asText());money(after,"paidAmount","400");money(after,"pendingCheckAmount","600");
        var paymentAfter=request("GET","/payments/"+paymentId,null,200);assertThat(paymentAfter.get("clientId").asText()).isEqualTo(target);assertThat(paymentAfter.get("parts")).isEqualTo(paid.get("parts"));
        var summary=request("GET","/clients/"+target+"/financial-summary",null,200).get("currencies").get(0);money(summary,"contracted","1000");money(summary,"collected","400");money(summary,"outstandingAmount","600");
        assertThat(jdbc.queryForObject("select count(*) from client_merge_records where merge_id in (select id from client_merges where source_client_id=?)",Integer.class,source)).isGreaterThan(4);
        request("POST","/projects",obj("clientId",source,"name","Invalid merged owner","projectCode","MERGED-"+UUID.randomUUID(),"status","PLANNED","priority","NORMAL","currency","ILS"),409);
        assertThat(request("GET","/clients?q="+java.net.URLEncoder.encode(sourceBefore.get("displayName").asText(),java.nio.charset.StandardCharsets.UTF_8),null,200).get("totalElements").asInt()).isZero();
        request("GET","/clients/"+target+"/merge-preview?targetId="+target,null,400);
    }
    @Test void invoiceAutomationIsOptInIdempotentAndNeverRecordsPaymentsOrAdvancesPeriods() throws Exception {
        String c=client(),p=project(c),service=service(p,"Automated hosting","120","MONTHLY");
        var subscription=request("POST","/subscriptions/from-service",obj("projectServiceId",service,"startDate",dates.today().minusMonths(1),"autoRenew",true),201);String id=subscription.get("id").asText();UUID subscriptionId=UUID.fromString(id);
        assertThat(subscription.get("invoiceAutomationEnabled").asBoolean()).isFalse();assertThat(subscriptionService.scheduledInvoice(subscriptionId)).isNull();
        var policy=request("PUT","/subscriptions/"+id+"/invoice-automation",obj("version",subscription.get("version").asLong(),"enabled",true,"leadDays",7,"dueDays",7),200);assertThat(policy.get("enabled").asBoolean()).isTrue();
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        var first=subscriptionService.scheduledInvoice(subscriptionId);var repeated=subscriptionService.scheduledInvoice(subscriptionId);assertThat(first.id()).isEqualTo(repeated.id());assertThat(first.paidAmount()).isZero();
        var httpInvoice=request("POST","/subscriptions/"+id+"/invoice-automation/run",null,200);assertThat(httpInvoice.get("id").asText()).isEqualTo(first.id().toString());
        var after=request("GET","/subscriptions/"+id,null,200);assertThat(after.get("currentPeriodEnd")).isEqualTo(subscription.get("currentPeriodEnd"));assertThat(jdbc.queryForObject("select count(*) from payments where client_id=?",Integer.class,c)).isZero();
        request("POST","/invoices/"+first.id()+"/void",obj("reason","Client cancelled invoice; manual review"),200);assertThat(subscriptionService.scheduledInvoice(subscriptionId).status()).isEqualTo("VOID");
        assertThat(automation.candidates(dates.today())).doesNotContain(subscriptionId);
        request("PUT","/subscriptions/"+id+"/invoice-automation",obj("version",after.get("version").asLong(),"enabled",false,"leadDays",7,"dueDays",7),200);assertThat(subscriptionService.scheduledInvoice(subscriptionId)).isNull();
    }
    @Test void mergeAndBillingControlsRejectUnprivilegedRolesAndAnonymousRequests() throws Exception {
        String source=client(),target=client(),p=project(source);
        for(String path:List.of("/clients/"+source+"/merge-preview?targetId="+target,"/projects/"+p+"/billing-plan")) {
            assertThat(mvc.perform(get("/api/v1"+path)).andReturn().getResponse().getStatus()).isEqualTo(401);
            assertThat(mvc.perform(get("/api/v1"+path).with(user(actor).authorities(new SimpleGrantedAuthority("CRM_READ"),new SimpleGrantedAuthority("CRM_WRITE"),new SimpleGrantedAuthority("WORK_READ"),new SimpleGrantedAuthority("WORK_WRITE")))).andReturn().getResponse().getStatus()).isEqualTo(403);
        }
        assertThat(mvc.perform(get("/api/v1/clients/"+source+"/merge-preview").param("targetId",target).with(user(actor).authorities(new SimpleGrantedAuthority("FINANCE_WRITE"),new SimpleGrantedAuthority("FINANCE_READ"),new SimpleGrantedAuthority("CRM_READ")))).andReturn().getResponse().getStatus()).isEqualTo(403);
    }

    @Test void duplicateWarningsNormalizeContactDetailsAndClientOverviewSupportsReadOnlyTransactions() throws Exception {
        String suffix=UUID.randomUUID().toString();String email="contact-"+suffix+"@example.test";
        var c=request("POST","/clients",obj("displayName","Studio  Example "+suffix,"email",email.toUpperCase(java.util.Locale.ROOT),"phone","+970 (598) "+suffix.substring(0,8).replaceAll("[^0-9]","")+" 12 34","status","ACTIVE","preferredCurrency","ILS"),201);String id=c.get("id").asText();
        var byEmail=request("GET","/clients/duplicates?email="+email,null,200);assertThat(byEmail.get(0).get("id").asText()).isEqualTo(id);assertThat(byEmail.get(0).get("match").asText()).isEqualTo("EMAIL");
        String normalizedPhone=c.get("phone").asText().replaceAll("[^0-9]","");
        var byPhone=request("GET","/clients/duplicates?phone="+normalizedPhone,null,200);assertThat(byPhone.toString()).contains(id);
        String normalizedName=java.net.URLEncoder.encode("studio example "+suffix,java.nio.charset.StandardCharsets.UTF_8);
        assertThat(request("GET","/clients/duplicates?name="+normalizedName,null,200).get(0).get("id").asText()).isEqualTo(id);
        assertThat(request("GET","/clients/duplicates?email="+email+"&excludeId="+id,null,200).size()).isZero();
        request("GET","/clients/"+id+"/summary",null,200);
        assertThat(request("GET","/clients/"+id,null,200).get("mergedIntoId").isNull()).isTrue();
    }

}
