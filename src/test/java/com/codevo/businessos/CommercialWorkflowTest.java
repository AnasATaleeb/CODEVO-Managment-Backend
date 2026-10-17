package com.codevo.businessos;

import com.codevo.businessos.common.BusinessDates;
import com.codevo.businessos.project.ProjectCommercialPolicy;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Uses the existing real filter-chain fixtures against an isolated native MySQL schema. */
@SpringBootTest(properties={"app.seed=false","app.secure-cookies=false","app.bootstrap-owner-email=acceptance-owner@example.test",
    "app.bootstrap-owner-password=Test-only-owner-password-2026","app.jwt-secret=test-only-jwt-key-with-more-than-thirty-two-bytes",
    "app.jobs.initial-delay-ms=3600000","springdoc.api-docs.enabled=false","springdoc.swagger-ui.enabled=false"})
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CommercialWorkflowTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired BusinessDates dates;
    private final MySqlAcceptanceTest fixture=new MySqlAcceptanceTest();
    @DynamicPropertySource static void database(DynamicPropertyRegistry p) { MySqlAcceptanceTest.database(p); }
    @BeforeAll void setUp() throws Exception {
        fixture.mvc=mvc;fixture.json=json;fixture.jdbc=jdbc;fixture.dates=dates;
        fixture.owner=fixture.login("acceptance-owner@example.test","Test-only-owner-password-2026").token();
    }
    private Map<String,Object> obj(Object... fields) {return fixture.obj(fields);}
    private JsonNode req(String method,String path,Object data,int status) throws Exception {return fixture.req(method,path,fixture.owner,data,status);}
    private String catalog() throws Exception {
        return req("POST","/services",obj("name","Commercial "+UUID.randomUUID(),"code","COMM-"+UUID.randomUUID(),"category","Development","defaultBillingType","ONE_TIME","defaultPrice","100","defaultCost","10","defaultCurrency","USD","active",true),201).get("id").asText();
    }
    private Map<String,Object> source(String catalog,String name,String price,String cost,String quantity,String billing) {
        return obj("serviceCatalogItemId",catalog,"customName",name,"billingType",billing,"unitPrice",price,"estimatedUnitCost",cost,"currency","USD","quantity",quantity,"status","PLANNED","recurring",!billing.equals("ONE_TIME"));
    }
    private Map<String,Object> projectData(String client,String code,List<Map<String,Object>> sources) {
        return obj("clientId",client,"name","Commercial platform "+UUID.randomUUID(),"projectCode",code,"status","IN_PROGRESS","priority","NORMAL","currency","USD","services",sources);
    }
    private Map<String,Object> invoiceData(String status,List<Map<String,Object>> services) {
        return obj("status",status,"issueDate",dates.today().toString(),"dueDate",dates.today().plusDays(7).toString(),"currency","USD","discount","0","tax","0","services",services);
    }
    private JsonNode onlySource(String project) throws Exception {return req("GET","/project-services?projectId="+project,null,200).get("content").get(0);}

    @Test void cancellingAllServicesDoesNotReviveLegacyEstimatesAndCatalogAcceptsQuarterly() throws Exception {
        String client=fixture.client().get("id").asText(),catalog=catalog();
        var data=projectData(client,"CANCEL-"+UUID.randomUUID(),List.of(source(catalog,"Cancelled scope","300","10","1","ONE_TIME")));
        data.put("estimatedPrice","9999");var project=req("POST","/projects",data,201);
        var service=onlySource(project.get("id").asText());
        var update=source(catalog,"Cancelled scope","300","10","1","ONE_TIME");update.put("projectId",project.get("id").asText());update.put("status","CANCELLED");update.put("version",service.get("version").asLong());
        req("PATCH","/project-services/"+service.get("id").asText(),update,200);
        var after=req("GET","/projects/"+project.get("id").asText(),null,200);
        assertThat(after.get("financialSource").asText()).isEqualTo("SERVICES");assertThat(after.get("calculatedContractValue").decimalValue()).isZero();assertThat(after.get("estimatedPrice").decimalValue()).isEqualByComparingTo("9999");
        req("POST","/services",obj("name","Quarterly catalog","code","Q-"+UUID.randomUUID(),"category","Hosting","defaultBillingType","QUARTERLY","defaultPrice","90","defaultCost","10","defaultCurrency","USD","active",true),201);
    }

    @Test void staffCanUpdateDeliveryWithoutReadingOrOverwritingCommercialAmounts() throws Exception {
        String client=fixture.client().get("id").asText(),catalog=catalog();
        var project=req("POST","/projects",projectData(client,"PRIVATE-"+UUID.randomUUID(),List.of(source(catalog,"Private agreement","300","10","1","ONE_TIME"))),201);
        String projectId=project.get("id").asText();var service=onlySource(projectId);
        String email="commercial-staff-"+UUID.randomUUID()+"@example.test",password="Test-only-staff-password-2026";
        req("POST","/users",obj("name","Commercial staff","email",email,"password",password,"role","STAFF"),201);
        String staff=fixture.login(email,password).token();
        var hidden=fixture.req("GET","/projects/"+projectId,staff,null,200);
        assertThat(hidden.get("estimatedPrice").isNull()).isTrue();assertThat(hidden.get("calculatedContractValue").isNull()).isTrue();assertThat(hidden.get("commercialTotals").size()).isZero();
        for(String path:List.of("/project-services/"+service.get("id").asText(),"/services/"+catalog)) {
            var row=fixture.req("GET",path,staff,null,200);
            for(String field:List.of("price","cost","unitPrice","estimatedUnitCost","agreedAmount","invoicedAmount","defaultPrice","defaultCost"))
                if(row.has(field))assertThat(row.get(field).isNull()).as(field+" is hidden").isTrue();
        }
        var update=source(catalog,"Private agreement","999","0","1","ONE_TIME");update.put("projectId",projectId);update.put("version",service.get("version").asLong());update.put("status","DELIVERED");
        fixture.req("PATCH","/project-services/"+service.get("id").asText(),staff,update,403);
        update.remove("unitPrice");update.remove("estimatedUnitCost");
        fixture.req("PATCH","/project-services/"+service.get("id").asText(),staff,update,200);
        var after=onlySource(projectId);assertThat(after.get("status").asText()).isEqualTo("DELIVERED");assertThat(after.get("unitPrice").decimalValue()).isEqualByComparingTo("300");assertThat(after.get("estimatedUnitCost").decimalValue()).isEqualByComparingTo("10");
    }

    @Test void createsProjectWithServiceTotalsAtomicallyAndPreservesHistoricalInvoiceSnapshot() throws Exception {
        String client=fixture.client().get("id").asText(),catalog=catalog();
        var request=projectData(client,"COMM-"+UUID.randomUUID(),List.of(source(catalog,"Website","3000","500","1","ONE_TIME"),source(catalog,"Apps","2000","300","1","ONE_TIME"),source(catalog,"Hosting","300","100","1","YEARLY")));
        request.put("estimatedPrice","9999");request.put("estimatedCost","999");
        var project=req("POST","/projects",request,201);String projectId=project.get("id").asText();
        assertThat(project.get("estimatedPrice").decimalValue()).isEqualByComparingTo("9999");
        assertThat(project.get("calculatedContractValue").decimalValue()).isEqualByComparingTo("5300");
        assertThat(project.get("calculatedEstimatedCost").decimalValue()).isEqualByComparingTo("900");
        assertThat(project.get("financialSource").asText()).isEqualTo("SERVICES");
        var rows=req("GET","/project-services?projectId="+projectId,null,200).get("content");
        JsonNode website=null,apps=null,hosting=null;for(var row:rows) {switch(row.get("customName").asText()) {case "Website"->website=row;case "Apps"->apps=row;case "Hosting"->hosting=row;default->throw new AssertionError();}}
        assertThat(hosting.get("subscriptionEligible").asBoolean()).isTrue();assertThat(hosting.get("billingInterval").asText()).isEqualTo("YEARLY");
        var invoice=req("POST","/projects/"+projectId+"/invoices",invoiceData("ISSUED",List.of(obj("projectServiceId",website.get("id").asText()),obj("projectServiceId",apps.get("id").asText()))),201);
        assertThat(invoice.get("total").decimalValue()).isEqualByComparingTo("5000");
        var changed=source(catalog,"Website scope expanded","3500","500","1","ONE_TIME");changed.put("projectId",projectId);changed.put("version",website.get("version").asLong());
        var updated=req("PATCH","/project-services/"+website.get("id").asText(),changed,200);
        assertThat(updated.get("invoicedAmount").decimalValue()).isEqualByComparingTo("3000");assertThat(updated.get("availableToInvoice").decimalValue()).isEqualByComparingTo("500");
        var historical=req("GET","/invoices/"+invoice.get("id").asText(),null,200);assertThat(historical.get("total").decimalValue()).isEqualByComparingTo("5000");assertThat(historical.get("items").toString()).contains("Website").doesNotContain("scope expanded");
        assertThat(req("GET","/projects/"+projectId,null,200).get("estimatedPrice").decimalValue()).isEqualByComparingTo("9999");
    }

    @Test void failedInlineServiceRollsBackProjectAndLegacyNoServiceEstimateRemains() throws Exception {
        String client=fixture.client().get("id").asText(),code="ROLLBACK-"+UUID.randomUUID();
        var data=projectData(client,code,List.of(source(catalog(),"Valid","100","10","1","ONE_TIME"),source(UUID.randomUUID().toString(),"Missing catalog","100","10","1","ONE_TIME")));
        req("POST","/projects",data,404);assertThat(jdbc.queryForObject("select count(*) from projects where project_code=?",Long.class,code)).isZero();
        var legacy=fixture.project(client);assertThat(legacy.get("financialSource").asText()).isEqualTo("LEGACY_ESTIMATE");assertThat(legacy.get("calculatedContractValue").decimalValue()).isEqualByComparingTo("1500");
        data=projectData(client,"EMPTY-"+UUID.randomUUID(),List.of());var empty=req("POST","/projects",data,201);assertThat(empty.get("estimatedPrice").decimalValue()).isZero();
    }

    @Test void draftReservesCapacityAndConcurrentPartialRequestsCannotDoubleInvoice() throws Exception {
        String client=fixture.client().get("id").asText();var project=req("POST","/projects",projectData(client,"RACE-"+UUID.randomUUID(),List.of(source(catalog(),"Milestone","300","10","3","ONE_TIME"))),201);
        String projectId=project.get("id").asText();var service=onlySource(projectId);String sourceId=service.get("id").asText();
        // Quantity three at 300 gives a 900 agreement; monetary milestones remain explicit snapshots.
        req("POST","/projects/"+projectId+"/invoices",invoiceData("ISSUED",List.of(obj("projectServiceId",sourceId,"amount","300"))),201);
        var draft=req("POST","/projects/"+projectId+"/invoices",invoiceData("DRAFT",List.of(obj("projectServiceId",sourceId,"amount","300"))),201);
        var balance=onlySource(projectId);assertThat(balance.get("invoicedAmount").decimalValue()).isEqualByComparingTo("300");assertThat(balance.get("draftReservedAmount").decimalValue()).isEqualByComparingTo("300");assertThat(balance.get("uninvoicedAmount").decimalValue()).isEqualByComparingTo("600");assertThat(balance.get("availableToInvoice").decimalValue()).isEqualByComparingTo("300");
        var attemptData=invoiceData("ISSUED",List.of(obj("projectServiceId",sourceId,"amount","300")));CountDownLatch start=new CountDownLatch(1);
        try(var executor=Executors.newFixedThreadPool(2)) {
            Callable<Integer> attempt=()->{start.await();return mvc.perform(post("/api/v1/projects/"+projectId+"/invoices").header("Authorization","Bearer "+fixture.owner).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(attemptData))).andReturn().getResponse().getStatus();};
            var a=executor.submit(attempt);var b=executor.submit(attempt);start.countDown();assertThat(List.of(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS))).containsExactlyInAnyOrder(201,409);
        }
        assertThat(onlySource(projectId).get("availableToInvoice").decimalValue()).isZero();
        req("POST","/invoices/"+draft.get("id").asText()+"/issue",null,200);assertThat(onlySource(projectId).get("billingStatus").asText()).isEqualTo("FULLY_INVOICED");
        req("POST","/invoices/"+draft.get("id").asText()+"/void",obj("reason","Cancelled milestone invoice"),200);assertThat(onlySource(projectId).get("availableToInvoice").decimalValue()).isEqualByComparingTo("300");
        var uninvoiced=req("GET","/project-services/uninvoiced?projectId="+projectId,null,200);assertThat(uninvoiced.get("totalElements").asLong()).isEqualTo(1);assertThat(uninvoiced.get("totals").get(0).get("amount").decimalValue()).isEqualByComparingTo("300");
    }

    @Test void guardsManualInvoiceBypassForeignServiceCurrencyAndConflictingAliases() throws Exception {
        String client=fixture.client().get("id").asText(),catalog=catalog();String projectId=req("POST","/projects",projectData(client,"GUARD-"+UUID.randomUUID(),List.of(source(catalog,"Service","100","10","1","ONE_TIME"))),201).get("id").asText();String sourceId=onlySource(projectId).get("id").asText();
        var manual=obj("clientId",client,"projectId",projectId,"status","ISSUED","issueDate",dates.today().toString(),"dueDate",dates.today().toString(),"currency","USD","discount","0","tax","0","items",List.of(obj("projectServiceId",sourceId,"description","Manual linked item","quantity","1","unitPrice","101")));
        req("POST","/invoices",manual,409);
        var invoice=invoiceData("ISSUED",List.of(obj("projectServiceId",sourceId)));invoice.put("currency","ILS");req("POST","/projects/"+projectId+"/invoices",invoice,400);
        String foreign=fixture.project(client).get("id").asText();req("POST","/projects/"+foreign+"/invoices",invoiceData("ISSUED",List.of(obj("projectServiceId",sourceId))),400);
        var alias=source(catalog,"Alias mismatch","100","10","1","ONE_TIME");alias.put("projectId",projectId);alias.put("price","200");req("POST","/project-services",alias,400);
        req("POST","/projects/"+projectId+"/invoices",invoiceData("ISSUED",List.of(obj("projectServiceId",sourceId),obj("projectServiceId",sourceId))),400);
        req("POST","/projects/"+projectId+"/invoices",invoiceData("ISSUED",List.of(obj("projectServiceId",sourceId,"amount","50","quantity","0.5"))),400);
    }

    @Test void mixedCurrencyTotalsStaySeparateAndQuarterlyBillingIsSupported() throws Exception {
        String client=fixture.client().get("id").asText(),catalog=catalog();var usd=source(catalog,"USD quarterly","100","10","2","QUARTERLY");var ils=source(catalog,"ILS service","300","20","1","ONE_TIME");ils.put("currency","ILS");
        var project=req("POST","/projects",projectData(client,"CURRENCIES-"+UUID.randomUUID(),List.of(usd,ils)),201);assertThat(project.get("commercialTotals").size()).isEqualTo(2);assertThat(project.get("calculatedContractValue").decimalValue()).isEqualByComparingTo("200");
        var rows=req("GET","/project-services?projectId="+project.get("id").asText(),null,200).get("content");for(var row:rows)if(row.get("customName").asText().equals("USD quarterly")){assertThat(row.get("billingInterval").asText()).isEqualTo("QUARTERLY");assertThat(row.get("subscriptionEligible").asBoolean()).isTrue();}
    }
}
