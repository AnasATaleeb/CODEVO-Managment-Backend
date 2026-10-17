package com.codevo.businessos;

import java.util.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.concurrent.*;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import tools.jackson.databind.*;
import com.codevo.businessos.common.BusinessDates;
import com.codevo.businessos.notification.AttentionJobs;

/** Native MySQL only. Supply TEST_DATABASE_* for an isolated schema ending in _test. */
@SpringBootTest(properties={"app.seed=false","app.secure-cookies=false","app.bootstrap-owner-email=acceptance-owner@example.test",
    "app.bootstrap-owner-password=Test-only-owner-password-2026","app.jwt-secret=test-only-jwt-key-with-more-than-thirty-two-bytes",
    "app.jobs.initial-delay-ms=3600000","springdoc.api-docs.enabled=false","springdoc.swagger-ui.enabled=false"})
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MySqlAcceptanceTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired BusinessDates dates;
    @Autowired AttentionJobs jobs;
    String owner;
    @DynamicPropertySource static void database(DynamicPropertyRegistry p) {
        TestDatabase.configure(p);
    }
    @BeforeAll void signInOwner() throws Exception { jdbc.update("update app_users set active=false where email<>?","acceptance-owner@example.test"); owner=login("acceptance-owner@example.test","Test-only-owner-password-2026").token; }
    Map<String,Object> obj(Object... entries) {
        Map<String,Object> m=new LinkedHashMap<>();for(int i=0;i<entries.length;i+=2)m.put((String)entries[i],entries[i+1]);return m;
    }
    JsonNode req(String method,String path,String token,Object body,int status) throws Exception {
        var request=org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(org.springframework.http.HttpMethod.valueOf(method),"/api/v1"+path.split("\\?",2)[0]);
        if(path.contains("?")) for(String pair:path.substring(path.indexOf('?')+1).split("&")) { var kv=pair.split("=",2);request.queryParam(kv[0],java.net.URLDecoder.decode(kv.length>1?kv[1]:"",java.nio.charset.StandardCharsets.UTF_8)); }
        if(token!=null)request.header("Authorization","Bearer "+token);
        if(body!=null)request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        var result=mvc.perform(request).andReturn();
        assertThat(result.getResponse().getStatus()).as(method+" "+path+" HTTP status").isEqualTo(status);
        String text=result.getResponse().getContentAsString();return text.isBlank()?json.readTree("null"):json.readTree(text);
    }
    record Login(String token,Cookie csrf,String csrfValue,Cookie refresh) {}
    Login login(String email,String password) throws Exception {
        var c=mvc.perform(get("/api/v1/auth/csrf")).andReturn();
        String token=json.readTree(c.getResponse().getContentAsString()).get("token").asText();Cookie csrf=c.getResponse().getCookie("XSRF-TOKEN");
        var r=mvc.perform(post("/api/v1/auth/login").cookie(csrf).header("X-XSRF-TOKEN",token).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(obj("email",email,"password",password)))).andReturn();
        assertThat(r.getResponse().getStatus()).as("login status").isEqualTo(200);
        return new Login(json.readTree(r.getResponse().getContentAsString()).get("accessToken").asText(),csrf,token,r.getResponse().getCookie("codevo_refresh"));
    }
    JsonNode client() throws Exception { return req("POST","/clients",owner,obj("displayName","Acceptance "+UUID.randomUUID(),"status","ACTIVE","preferredCurrency","USD"),201); }
    JsonNode project(String client) throws Exception {return req("POST","/projects",owner,obj("clientId",client,"name","Acceptance platform "+UUID.randomUUID(),"projectCode","TEST-"+UUID.randomUUID(),"status","IN_PROGRESS","priority","HIGH","startDate",dates.today().toString(),"deadline",dates.today().plusDays(2).toString(),"estimatedPrice","1500","estimatedCost","200","currency","USD"),201);}
    JsonNode invoice(String client,String project,String total,LocalDate due) throws Exception {return req("POST","/invoices",owner,obj("clientId",client,"projectId",project,"status","ISSUED","issueDate",due.isBefore(dates.today())?due.minusDays(1).toString():dates.today().toString(),"dueDate",due.toString(),"currency","USD","tax","0","discount","0","items",List.of(obj("description","Website build","quantity",1,"unitPrice",new BigDecimal(total).divide(BigDecimal.valueOf(2)).toPlainString()),obj("description","Mobile milestone","quantity",1,"unitPrice",new BigDecimal(total).divide(BigDecimal.valueOf(2)).toPlainString()))),201);}
    Map<String,Object> part(String method,String amount,String currency) {return obj("method",method,"amount",amount,"currency",currency,"bankName",method.equals("BANK_TRANSFER")?"Acceptance Bank":null);}
    JsonNode payment(String client,String invoice,List<Map<String,Object>> parts) throws Exception {return req("POST","/payments",owner,obj("clientId",client,"invoiceId",invoice,"paymentDate",dates.today().toString(),"parts",parts),201);}
    JsonNode summary(String client) throws Exception {return req("GET","/dashboard/summary?clientId="+client,owner,null,200);}
    BigDecimal total(JsonNode n,String key,String currency) {for(var row:n.path(key))if(row.path("currency").asText().equals(currency))return row.path("amount").decimalValue();return BigDecimal.ZERO;}

    @Test void migrationsValidateOnNativeMySql() {
        assertThat(jdbc.queryForObject("select version()",String.class)).startsWith("8.");
        assertThat(jdbc.queryForObject("select count(*) from flyway_schema_history where success=true",Integer.class)).isGreaterThanOrEqualTo(3);
        assertThat(jdbc.queryForObject("select count(*) from information_schema.referential_constraints where constraint_schema=database()",Integer.class)).isGreaterThan(20);
    }
    @Test void scenarioAClientProjectServicesTasksAndDeadline() throws Exception {
        var c=client();var p=project(c.get("id").asText());
        for(String name:List.of("Website","Mobile app")) {
            var s=req("POST","/services",owner,obj("name",name+UUID.randomUUID(),"code",name.equals("Website")?"WEBSITE-"+UUID.randomUUID():"MOBILE_APP-"+UUID.randomUUID(),"category","Development","defaultBillingType","ONE_TIME","defaultPrice","500","defaultCost","100","defaultCurrency","USD","active",true),201);
            req("POST","/project-services",owner,obj("projectId",p.get("id").asText(),"serviceCatalogItemId",s.get("id").asText(),"customName",name,"billingType","ONE_TIME","price","500","cost","100","currency","USD","quantity",1,"status","IN_PROGRESS","recurring",false),201);
        }
        req("POST","/tasks",owner,obj("projectId",p.get("id").asText(),"clientId",c.get("id").asText(),"title","Acceptance task","status","TODO","priority","HIGH","dueAt",Instant.now().plusSeconds(86400).toString(),"sortOrder",0),201);
        assertThat(req("GET","/project-services?projectId="+p.get("id").asText(),owner,null,200).get("totalElements").asInt()).isEqualTo(2);
        assertThat(req("GET","/projects/"+p.get("id").asText()+"/progress",owner,null,200).get("total").asInt()).isEqualTo(1);
        assertThat(req("GET","/dashboard/attention",owner,null,200).toString()).contains(p.get("id").asText());
        assertThat(req("GET","/calendar?from="+dates.today()+"&to="+dates.today().plusDays(3),owner,null,200).toString()).contains(p.get("id").asText());
    }
    @Test void calendarUsesOrganizationDayBoundariesAndKeepsAllDayDates() throws Exception {
        String originalZone=jdbc.queryForObject("select timezone from organization_settings where id=1",String.class);
        try {
            for(String zone:List.of("Asia/Hebron","America/New_York")) {
                jdbc.update("update organization_settings set timezone=? where id=1",zone);
                String c=client().get("id").asText();var p=project(c);LocalDate day=LocalDate.parse(p.get("deadline").asText());
                List<String> taskIds=new ArrayList<>();
                for(LocalTime time:List.of(LocalTime.of(0,30),LocalTime.of(23,30))) {
                    String instant=day.atTime(time).atZone(ZoneId.of(zone)).toInstant().toString();
                    taskIds.add(req("POST","/tasks",owner,obj("clientId",c,"projectId",p.get("id").asText(),"title","Timezone boundary "+time,"status","TODO","priority","NORMAL","dueAt",instant,"sortOrder",0),201).get("id").asText());
                }
                var events=req("GET","/calendar?from="+day+"&to="+day+"&clientId="+c,owner,null,200);
                assertThat(events.size()).isEqualTo(3);
                for(var event:events) {
                    if(event.get("type").asText().equals("PROJECT_DEADLINE")) {
                        assertThat(event.get("start_at").asText()).isEqualTo(day.toString());assertThat(event.get("all_day").asBoolean()).isTrue();
                    } else {
                        assertThat(taskIds).contains(event.get("id").asText());assertThat(event.get("all_day").asBoolean()).isFalse();
                        assertThat(Instant.parse(event.get("start_at").asText()).atZone(ZoneId.of(zone)).toLocalDate()).isEqualTo(day);
                    }
                }
                assertThat(req("GET","/calendar?from="+day.minusDays(1)+"&to="+day.minusDays(1)+"&clientId="+c,owner,null,200).size()).isZero();
                assertThat(req("GET","/calendar?from="+day.plusDays(1)+"&to="+day.plusDays(1)+"&clientId="+c,owner,null,200).size()).isZero();
            }
        } finally { jdbc.update("update organization_settings set timezone=? where id=1",originalZone); }
    }
    @Test void scenarioBPartialThenFinalPaymentUpdatesInvoiceAndRevenue() throws Exception {
        String c=client().get("id").asText();var i=invoice(c,null,"1000",dates.today().plusDays(10));
        assertThat(i.get("items").size()).isEqualTo(2);
        payment(c,i.get("id").asText(),List.of(part("CASH","400","USD")));
        var partial=req("GET","/invoices/"+i.get("id").asText(),owner,null,200);
        assertThat(partial.get("status").asText()).isEqualTo("PARTIALLY_PAID");assertThat(partial.get("balanceDue").decimalValue()).isEqualByComparingTo("600");
        payment(c,i.get("id").asText(),List.of(part("BANK_TRANSFER","600","USD")));
        assertThat(req("GET","/invoices/"+i.get("id").asText(),owner,null,200).get("status").asText()).isEqualTo("PAID");
        assertThat(total(summary(c),"collected","USD")).isEqualByComparingTo("1000");
    }
    @Test void scenarioB2CompoundPaymentKeepsEveryPartCheckAndCurrencyIndependent() throws Exception {
        String c=client().get("id").asText();List<Map<String,Object>> parts=new ArrayList<>();
        var bank=part("BANK_TRANSFER","9000","ILS");bank.put("bankName","Arab Bank");parts.add(bank);
        for(int x=0;x<4;x++){var check=part("CHECK","1000","ILS");check.put("check",obj("checkNumber","TEST-"+UUID.randomUUID(),"bankName","Arab Bank","issuerName","Acceptance issuer","receivedDate",dates.today().toString(),"dueDate",dates.today().plusDays(x+1).toString()));parts.add(check);}
        var palestine=part("BANK_TRANSFER","350","ILS");palestine.put("bankName","Bank of Palestine");parts.add(palestine);
        parts.add(part("CASH","150","ILS"));parts.add(part("CASH","50","ILS"));parts.add(part("CASH","50","JOD"));parts.add(part("CASH","150","USD"));
        var p=payment(c,null,parts);assertThat(p.get("parts").size()).isEqualTo(10);
        List<JsonNode> checks=new ArrayList<>();for(var row:p.get("parts"))if(!row.get("check").isNull())checks.add(row.get("check"));assertThat(checks).hasSize(4);
        var before=summary(c);assertThat(total(before,"collected","ILS")).isEqualByComparingTo("9550");assertThat(total(before,"pendingChecks","ILS")).isEqualByComparingTo("4000");
        assertThat(total(before,"collected","JOD")).isEqualByComparingTo("50");assertThat(total(before,"collected","USD")).isEqualByComparingTo("150");
        var clear=checks.get(0);req("POST","/checks/"+clear.get("id").asText()+"/status",owner,obj("status","CLEARED","date",dates.today().toString(),"version",clear.get("version").asLong()),200);
        var bounce=checks.get(1);req("POST","/checks/"+bounce.get("id").asText()+"/status",owner,obj("status","BOUNCED","date",dates.today().toString(),"reason","Bank rejected payment","version",bounce.get("version").asLong()),200);
        var after=summary(c);assertThat(total(after,"collected","ILS")).isEqualByComparingTo("10550");assertThat(total(after,"pendingChecks","ILS")).isEqualByComparingTo("2000");assertThat(total(after,"bouncedChecks","ILS")).isEqualByComparingTo("1000");
        req("POST","/checks/"+clear.get("id").asText()+"/status",owner,obj("status","BOUNCED","date",dates.today().toString(),"reason","Invalid direct change","version",1),409);
        req("POST","/checks/"+clear.get("id").asText()+"/reverse",owner,obj("reason","Correct mistaken clearing"),200);
        assertThat(total(summary(c),"collected","ILS")).isEqualByComparingTo("9550");
        req("POST","/payments/"+p.get("id").asText()+"/reverse",owner,obj("reason","Correct full payment event"),200);
        assertThat(total(summary(c),"collected","ILS")).isZero();assertThat(total(summary(c),"pendingChecks","ILS")).isZero();
    }
    JsonNode subscription(String client,String name,String interval,String amount,LocalDate end) throws Exception {
        return req("POST","/subscriptions",owner,obj("clientId",client,"name",name,"status","ACTIVE","billingInterval",interval,"amount",amount,"currency","USD","startDate",dates.today().minusMonths(1).toString(),"currentPeriodStart",dates.today().toString(),"currentPeriodEnd",end.toString(),"renewalDate",end.toString(),"autoRenew",true,"gracePeriodDays",3),201);
    }
    @Test void scenariosCAndDMonthlyYearlyRevenueRenewalsAndWarnings() throws Exception {
        String c=client().get("id").asText();var monthly=subscription(c,"Acceptance hosting","MONTHLY","85",dates.today().plusDays(5));subscription(c,"Acceptance yearly","YEARLY","1200",dates.today().plusYears(1));
        var summary=summary(c);assertThat(total(summary,"mrr","USD")).isEqualByComparingTo("185");assertThat(total(summary,"arr","USD")).isEqualByComparingTo("2220");
        assertThat(req("GET","/subscriptions?clientId="+c,owner,null,200).get("totalElements").asInt()).isEqualTo(2);
        assertThat(req("GET","/dashboard/attention",owner,null,200).toString()).contains(monthly.get("id").asText());
        var renewed=req("POST","/subscriptions/"+monthly.get("id").asText()+"/renew",owner,obj("version",monthly.get("version").asLong(),"reason","Client approved next billing period"),200);
        assertThat(LocalDate.parse(renewed.get("currentPeriodStart").asText())).isEqualTo(dates.today().plusDays(6));
        req("POST","/subscriptions/"+monthly.get("id").asText()+"/renew",owner,obj("version",monthly.get("version").asLong(),"reason","Stale renewal attempt"),409);
    }
    @Test void scenarioEExpenseAndClientProjectProfitIncludesMixedCurrency() throws Exception {
        String c=client().get("id").asText();String p=project(c).get("id").asText();String i=invoice(c,p,"1000",dates.today().plusDays(10)).get("id").asText();
        var usd=part("CASH","400","USD");var ils=part("BANK_TRANSFER","200","ILS");ils.put("allocationExchangeRate","0.25");
        payment(c,i,List.of(usd,ils));
        req("POST","/expenses",owner,obj("clientId",c,"projectId",p,"category","HOSTING","description","Acceptance infrastructure","amount","100","currency","USD","expenseDate",dates.today().toString(),"recurring",false),201);
        var s=summary(c);assertThat(total(s,"profit","USD")).isEqualByComparingTo("300");assertThat(total(s,"profit","ILS")).isEqualByComparingTo("200");
        var contribution=req("GET","/analytics/clients?clientId="+c,owner,null,200);assertThat(contribution.toString()).contains("300");
        var projects=req("GET","/analytics/projects?projectId="+p,owner,null,200);assertThat(projects.size()).isEqualTo(2);assertThat(projects.toString()).contains("ILS");
        assertThat(req("GET","/payments?projectId="+p,owner,null,200).get("totalElements").asInt()).isEqualTo(1);
        assertThat(req("GET","/payments?projectId="+UUID.randomUUID(),owner,null,200).get("totalElements").asInt()).isZero();
    }
    @Test void scenarioFOverdueInvoiceOutstandingAndAttention() throws Exception {
        String c=client().get("id").asText();var i=invoice(c,null,"200",dates.today().minusDays(1));assertThat(i.get("status").asText()).isEqualTo("OVERDUE");
        assertThat(total(summary(c),"outstanding","USD")).isEqualByComparingTo("200");assertThat(total(req("GET","/dashboard/summary?clientId="+c+"&from="+dates.today()+"&to="+dates.today(),owner,null,200),"outstanding","USD")).isEqualByComparingTo("200");var attention=req("GET","/dashboard/attention",owner,null,200);assertThat(attention.toString()).contains(i.get("id").asText());
        for(var item:attention)if(item.get("id").asText().equals(i.get("id").asText()))assertThat(item.get("title").asText()).contains(req("GET","/clients/"+c,owner,null,200).get("displayName").asText());
    }
    @Test void scenarioGLeadPipelineConversionIsIdempotentAndRetainsHistory() throws Exception {
        var data=obj("name","Acceptance lead "+UUID.randomUUID(),"companyName","Acceptance company","estimatedValue","500","currency","USD","stage","NEW","priority","NORMAL");
        var l=req("POST","/leads",owner,data,201);String id=l.get("id").asText();
        req("POST","/leads/"+id+"/convert",owner,obj("projectName",null),409);
        for(String stage:List.of("CONTACTED","QUALIFIED","PROPOSAL_SENT","NEGOTIATION","WON")){data.put("stage",stage);data.put("version",l.get("version").asLong());l=req("PATCH","/leads/"+id,owner,data,200);}
        var conversion=req("POST","/leads/"+id+"/convert",owner,obj("projectName","Converted first project"),200);assertThat(conversion.get("projectId").isNull()).isFalse();
        assertThat(req("POST","/leads/"+id+"/convert",owner,obj("projectName",null),200).get("clientId")).isEqualTo(conversion.get("clientId"));
        assertThat(req("GET","/leads/"+id,owner,null,200).get("convertedClientId")).isEqualTo(conversion.get("clientId"));
        assertThat(req("GET","/activity?entityId="+id,owner,null,200).get("totalElements").asInt()).isGreaterThan(5);
    }
    @Test void scenarioHDirectApiAuthorizationAndRevokedDisabledSessions() throws Exception {
        String c=client().get("id").asText();
        for(String role:List.of("STAFF","ACCOUNTANT")) {
            String email=role.toLowerCase()+UUID.randomUUID()+"@example.test";var user=req("POST","/users",owner,obj("email",email,"name","Acceptance "+role,"password","Test-only-account-password","role",role),201);
            var login=login(email,"Test-only-account-password");
            req("GET","/clients",login.token,null,200);req("GET","/settings",login.token,null,200);
            req("POST","/users",login.token,obj("email","forbidden@example.test","name","Forbidden","password","Test-only-account-password","role","OWNER"),403);
            req("POST","/payments",login.token,obj("clientId",c,"paymentDate",dates.today().toString(),"parts",List.of(part("CASH","25","USD"))),role.equals("STAFF")?403:201);
            req("GET","/analytics/revenue",login.token,null,role.equals("STAFF")?403:200);
            if(role.equals("STAFF")){assertThat(req("GET","/dashboard/summary",login.token,null,200).has("collected")).isFalse();assertThat(req("GET","/search?q=INV",login.token,null,200).toString()).doesNotContain("invoices");}
            req("POST","/users/"+user.get("id").asText()+"/disable",owner,obj("reason","Acceptance account finished"),204);req("GET","/clients",login.token,null,401);
        }
        req("GET","/clients",null,null,401);
        var me=req("GET","/auth/me",owner,null,200);req("POST","/users/"+me.get("id").asText()+"/disable",owner,obj("reason","Last owner must remain"),409);
    }
    @Test void scenarioISearchFindsCorrectClientProjectInvoiceRoutesAndEscapesWildcard() throws Exception {
        var c=client();var p=project(c.get("id").asText());var i=invoice(c.get("id").asText(),p.get("id").asText(),"50",dates.today());
        for(var record:List.of(c,p,i)) {
            String term=record.has("displayName")?record.get("displayName").asText():record.has("invoiceNumber")?record.get("invoiceNumber").asText():record.get("name").asText();
            var results=req("GET","/search?q="+java.net.URLEncoder.encode(term,java.nio.charset.StandardCharsets.UTF_8),owner,null,200);assertThat(results.toString()).contains(record.get("id").asText());
        }
        assertThat(req("GET","/search?q=%25%25%25",owner,null,200).size()).isZero();
    }
    @Test void invalidMoneyUnknownFieldsStaleEditsAndOverpaymentRollback() throws Exception {
        var c=client();String id=c.get("id").asText();var i=invoice(id,null,"100",dates.today().plusDays(1));
        req("POST","/payments",owner,obj("clientId",id,"invoiceId",i.get("id").asText(),"paymentDate",dates.today().toString(),"parts",List.of(part("CASH","100.001","USD"))),400);
        req("POST","/payments",owner,obj("clientId",id,"invoiceId",i.get("id").asText(),"paymentDate",dates.today().toString(),"parts",List.of(part("CASH","101","USD"))),409);
        assertThat(req("GET","/payments?clientId="+id,owner,null,200).get("totalElements").asInt()).isZero();
        req("POST","/clients",owner,obj("displayName","Unsafe","status","ACTIVE","preferredCurrency","USD","role","OWNER"),400);
        var edit=obj("displayName","Updated acceptance client","status","ACTIVE","preferredCurrency","USD","version",c.get("version").asLong());req("PATCH","/clients/"+id,owner,edit,200);req("PATCH","/clients/"+id,owner,edit,409);
        req("GET","/clients?size=101",owner,null,400);req("GET","/clients?sort=passwordHash",owner,null,400);
    }
    @Test void concurrentAllocationsCannotOverpayInvoice() throws Exception {
        String c=client().get("id").asText();String i=invoice(c,null,"100",dates.today().plusDays(1)).get("id").asText();
        var data=obj("clientId",c,"invoiceId",i,"paymentDate",dates.today().toString(),"parts",List.of(part("CASH","60","USD")));
        CountDownLatch start=new CountDownLatch(1);try(var executor=Executors.newFixedThreadPool(2)) {
            Callable<Integer> attempt=()->{start.await();return mvc.perform(post("/api/v1/payments").header("Authorization","Bearer "+owner).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(data))).andReturn().getResponse().getStatus();};
            var a=executor.submit(attempt);var b=executor.submit(attempt);start.countDown();assertThat(List.of(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS))).containsExactlyInAnyOrder(201,409);
        }
        assertThat(req("GET","/invoices/"+i,owner,null,200).get("paidAmount").decimalValue()).isEqualByComparingTo("60");
    }
    @Test void csrfRefreshRotationReuseAndLogoutRevokeWholeSession() throws Exception {
        assertThat(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}")).andReturn().getResponse().getStatus()).isEqualTo(403);
        var auth=login("acceptance-owner@example.test","Test-only-owner-password-2026");
        var refreshed=mvc.perform(post("/api/v1/auth/refresh").cookie(auth.csrf,auth.refresh).header("X-XSRF-TOKEN",auth.csrfValue)).andReturn();assertThat(refreshed.getResponse().getStatus()).isEqualTo(200);
        String newAccess=json.readTree(refreshed.getResponse().getContentAsString()).get("accessToken").asText();Cookie rotated=refreshed.getResponse().getCookie("codevo_refresh");
        assertThat(rotated.getValue()).isNotEqualTo(auth.refresh.getValue());
        assertThat(mvc.perform(post("/api/v1/auth/refresh").cookie(auth.csrf,auth.refresh).header("X-XSRF-TOKEN",auth.csrfValue)).andReturn().getResponse().getStatus()).isEqualTo(401);
        req("GET","/clients",newAccess,null,401);
        var fresh=login("acceptance-owner@example.test","Test-only-owner-password-2026");assertThat(mvc.perform(post("/api/v1/auth/logout").cookie(fresh.csrf,fresh.refresh).header("X-XSRF-TOKEN",fresh.csrfValue)).andReturn().getResponse().getStatus()).isEqualTo(204);req("GET","/clients",fresh.token,null,401);
    }
    @Test void checkCoverageFreesBouncedAllocationAndOnlyClearingCollects() throws Exception {
        String c=client().get("id").asText();String i=invoice(c,null,"100",dates.today().plusDays(1)).get("id").asText();
        var part=part("CHECK","60","USD");part.put("check",obj("checkNumber","COVERAGE-"+UUID.randomUUID(),"bankName","Coverage Bank","receivedDate",dates.today().toString(),"dueDate",dates.today().plusDays(10).toString()));
        var p=payment(c,i,List.of(part));var check=p.get("parts").get(0).get("check");
        var initial=req("GET","/invoices/"+i,owner,null,200);assertThat(initial.get("status").asText()).isEqualTo("ISSUED");assertThat(initial.get("balanceDue").decimalValue()).isEqualByComparingTo("100");assertThat(initial.get("pendingCheckAmount").decimalValue()).isEqualByComparingTo("60");
        req("POST","/payments",owner,obj("clientId",c,"invoiceId",i,"paymentDate",dates.today().toString(),"parts",List.of(part("CASH","50","USD"))),409);
        var bounced=req("POST","/checks/"+check.get("id").asText()+"/status",owner,obj("status","BOUNCED","date",dates.today().toString(),"reason","Bank returned pending check","version",0),200);assertThat(bounced.get("status").asText()).isEqualTo("BOUNCED");
        payment(c,i,List.of(part("CASH","100","USD")));assertThat(req("GET","/invoices/"+i,owner,null,200).get("status").asText()).isEqualTo("PAID");
    }
    @Test void scheduledExpirationAndOverdueTransitionsRetainAuditAndUtcDates() throws Exception {
        String c=client().get("id").asText();var i=invoice(c,null,"100",dates.today().plusDays(1));
        jdbc.update("update invoices set issue_date=date_sub(?,interval 1 day),due_date=?,status='ISSUED' where id=?",dates.today().minusDays(1),dates.today().minusDays(1),i.get("id").asText());
        var sub=subscription(c,"Expiration fixture","MONTHLY","85",dates.today().plusDays(1));
        jdbc.update("update subscriptions set current_period_start=date_sub(?,interval 1 day),current_period_end=?,grace_period_days=0,status='ACTIVE' where id=?",dates.today().minusDays(1),dates.today().minusDays(1),sub.get("id").asText());
        jobs.run();jobs.run();assertThat(req("GET","/invoices/"+i.get("id").asText(),owner,null,200).get("status").asText()).isEqualTo("OVERDUE");assertThat(req("GET","/subscriptions/"+sub.get("id").asText(),owner,null,200).get("status").asText()).isEqualTo("EXPIRED");
        var activity=req("GET","/activity?entityId="+sub.get("id").asText(),owner,null,200);assertThat(activity.get("content").get(0).get("created_at").asText()).endsWith("Z");
        assertThat(jdbc.queryForObject("select count(*) from activity_log where entity_id=? and action='EXPIRED'",Long.class,sub.get("id").asText())).isEqualTo(1L);
    }
    @Test void duplicatePhysicalChecksWithOmittedIssuerRollbackAndFxRatesAreRequired() throws Exception {
        String c=client().get("id").asText();var p=part("CHECK","10","USD");p.put("check",obj("checkNumber","DUPLICATE-"+UUID.randomUUID(),"bankName","Identity Bank","receivedDate",dates.today().toString(),"dueDate",dates.today().toString()));
        payment(c,null,List.of(p));req("POST","/payments",owner,obj("clientId",c,"paymentDate",dates.today().toString(),"parts",List.of(p)),409);
        assertThat(req("GET","/payments?clientId="+c,owner,null,200).get("totalElements").asInt()).isEqualTo(1);
        String invoice=invoice(c,null,"100",dates.today().plusDays(10)).get("id").asText();req("POST","/payments",owner,obj("clientId",c,"invoiceId",invoice,"paymentDate",dates.today().toString(),"parts",List.of(part("CASH","50","ILS"))),400);
    }
    @Test void notificationJobsAreIdempotentAndReadIsUserScoped() throws Exception {
        String c=client().get("id").asText();invoice(c,null,"100",dates.today().minusDays(2));jobs.run();long first=jdbc.queryForObject("select count(*) from notifications",Long.class);jobs.run();assertThat(jdbc.queryForObject("select count(*) from notifications",Long.class)).isEqualTo(first);
        var page=req("GET","/notifications?size=100",owner,null,200);assertThat(page.get("totalElements").asLong()).isPositive();
        String email="notifications"+UUID.randomUUID()+"@example.test";var otherUser=req("POST","/users",owner,obj("email",email,"name","Other owner","password","Test-only-account-password","role","OWNER"),201);var other=login(email,"Test-only-account-password");
        req("POST","/notifications/"+page.get("content").get(0).get("id").asText()+"/read",other.token,null,404);req("POST","/users/"+otherUser.get("id").asText()+"/disable",owner,obj("reason","Acceptance account finished"),204);
    }
    @Test void fileUploadsArePrivateAndRejectDisguisedExecutablesAndInvalidRelations() throws Exception {
        String c=client().get("id").asText();byte[] pdf="%PDF-1.7\nAcceptance attachment\n%%EOF".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var file=new MockMultipartFile("file","../../receipt.pdf","application/pdf",pdf);
        var r=mvc.perform(multipart("/api/v1/files").file(file).param("clientId",c).param("category","DOCUMENT").header("Authorization","Bearer "+owner)).andReturn();assertThat(r.getResponse().getStatus()).isEqualTo(201);
        var meta=json.readTree(r.getResponse().getContentAsString());String id=meta.get("id").asText();assertThat(meta.toString()).doesNotContain("storageKey");
        var downloaded=mvc.perform(get("/api/v1/files/"+id+"/download").header("Authorization","Bearer "+owner)).andReturn();assertThat(downloaded.getResponse().getContentAsByteArray()).isEqualTo(pdf);req("GET","/files/"+id+"/download",null,null,401);
        var bad=new MockMultipartFile("file","receipt.pdf","application/pdf",new byte[]{'M','Z',1,2,3});assertThat(mvc.perform(multipart("/api/v1/files").file(bad).param("clientId",c).param("category","DOCUMENT").header("Authorization","Bearer "+owner)).andReturn().getResponse().getStatus()).isEqualTo(400);
    }
}
