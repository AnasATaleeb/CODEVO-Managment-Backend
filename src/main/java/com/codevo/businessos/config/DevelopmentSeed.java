package com.codevo.businessos.config;
import java.time.*;
import java.util.*;
import java.math.BigDecimal;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import com.codevo.businessos.security.Permissions;
import com.codevo.businessos.common.Currency;
import com.codevo.businessos.common.BusinessDates;
import com.codevo.businessos.client.*;
import com.codevo.businessos.project.*;
import com.codevo.businessos.lead.*;
import com.codevo.businessos.task.*;
import com.codevo.businessos.servicecatalog.*;
import com.codevo.businessos.billing.invoice.*;
import com.codevo.businessos.billing.payment.*;
import com.codevo.businessos.billing.subscription.*;
import com.codevo.businessos.billing.expense.*;
@Component @Profile("dev") @Order(100)
public class DevelopmentSeed implements ApplicationRunner {
    private final AppProperties props;private final JdbcTemplate jdbc;private final ClientService clients;
    private final ProjectApplicationService projects;private final ServiceCatalogItemService catalog;private final ProjectServiceApplicationService services;
    private final TaskService tasks;private final LeadService leads;private final InvoiceService invoices;private final PaymentService payments;
    private final SubscriptionService subscriptions;private final ExpenseService expenses;private final Clock clock; private final BusinessDates dates;
    public DevelopmentSeed(AppProperties props,JdbcTemplate jdbc,ClientService clients,ProjectApplicationService projects,ServiceCatalogItemService catalog,
        ProjectServiceApplicationService services,TaskService tasks,LeadService leads,InvoiceService invoices,PaymentService payments,SubscriptionService subscriptions,ExpenseService expenses,Clock clock, BusinessDates dates) {
        this.props=props;this.jdbc=jdbc;this.clients=clients;this.projects=projects;this.catalog=catalog;this.services=services;this.tasks=tasks;this.leads=leads;
        this.invoices=invoices;this.payments=payments;this.subscriptions=subscriptions;this.expenses=expenses;this.clock=clock; this.dates=dates;
    }
    private BigDecimal amount(String value) { return new BigDecimal(value); }
    @Override @Transactional public void run(ApplicationArguments args) {
        if(!props.seed() || jdbc.queryForObject("select count(*) from clients",Long.class)>0) return;
        String owner=jdbc.query("select id from app_users where role='OWNER' and active=true limit 1",rs->rs.next()?rs.getString(1):null);
        if(owner==null) return;
        var authorities=Permissions.forRole("OWNER").stream().map(SimpleGrantedAuthority::new).toList();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(owner,null,authorities));
        try {
            LocalDate today=dates.today();
            var website=catalog.create(new ServiceCatalogItemDtos.Request("Website","WEBSITE","Development","Responsive websites","ONE_TIME",amount("700"),amount("200"),Currency.USD,true,null));
            var mobile=catalog.create(new ServiceCatalogItemDtos.Request("Mobile application","MOBILE_APP","Development","Native and cross-platform apps","ONE_TIME",amount("1200"),amount("400"),Currency.USD,true,null));
            catalog.create(new ServiceCatalogItemDtos.Request("Hosting & support","HOSTING","Infrastructure","Managed hosting","MONTHLY",amount("85"),amount("20"),Currency.USD,true,null));
            String[] names={"Demo · Olive Studio","Demo · Atlas Learning","Demo · Northstar Labs"};
            for(int index=0;index<names.length;index++) {
                var client=clients.create(new ClientDtos.Request(names[index],"Synthetic development company","Demo Contact","demo"+index+"@example.test",null,null,
                    "Palestine","Hebron",null,"Development seed","ACTIVE","Clearly fake development data. Do not use as real client records.",Currency.USD,"demo",UUID.fromString(owner),today.minusMonths(3),today.minusMonths(2),null));
                var project=projects.create(new ProjectDtos.Request(client.id(),index==0?"Demo · Commerce platform":index==1?"Demo · Learning portal":"Demo · Brand website","DEMO-00"+index,
                    "Synthetic project for local development","IN_PROGRESS",index==0?"HIGH":"NORMAL",today.minusDays(14),today.plusDays(index==0?2:10),null,UUID.fromString(owner),
                    amount("1900"),amount("600"),Currency.USD,null,null));
                var projectService=services.create(new ProjectServiceDtos.Request(project.id(),website.id(),"Website","Delivery scope","ONE_TIME",amount("700"),amount("200"),Currency.USD,BigDecimal.ONE,today,null,today.plusDays(2),"IN_PROGRESS",false,null,null));
                services.create(new ProjectServiceDtos.Request(project.id(),mobile.id(),"Mobile app","Companion app","ONE_TIME",amount("1200"),amount("400"),Currency.USD,BigDecimal.ONE,today,null,today.plusDays(10),"PLANNED",false,null,null));
                tasks.create(new TaskDtos.Request(project.id(),client.id(),"Demo · Review "+(index==0?"checkout flow":index==1?"course content":"brand assets"),"Synthetic task","IN_PROGRESS","HIGH",UUID.fromString(owner),clock.instant().plusSeconds(86400*(index+1)),null,index,null));
                var invoice=invoices.create(new InvoiceDtos.Request(client.id(),project.id(),"ISSUED",today.minusDays(20),index==0?today.minusDays(2):today.plusDays(10),Currency.USD,BigDecimal.ZERO,BigDecimal.ZERO,
                    "Development seed invoice",List.of(new InvoiceDtos.ItemRequest(projectService.id(),"Website build",BigDecimal.ONE,amount("700")),new InvoiceDtos.ItemRequest(null,"Mobile app milestone",BigDecimal.ONE,amount("1200")))));
                payments.create(new PaymentDtos.Request(client.id(),invoice.id(),today.minusDays(index+1),"Development seed payment",
                    List.of(new PaymentDtos.PartRequest("BANK_TRANSFER",amount("400"),Currency.USD,"Demo Bank","DEMO-REF-"+index,today.minusDays(index+1),null,null,null,null,null))));
                subscriptions.create(new SubscriptionDtos.Request(client.id(),project.id(),null,"Demo · Hosting & support","ACTIVE","MONTHLY",amount("85"),Currency.USD,today.minusMonths(1),today,today.plusMonths(1),today.plusDays(index==0?5:20),true,3,null,null,"Development seed",null));
                expenses.create(new ExpenseDtos.Request(client.id(),project.id(),"HOSTING","Demo · Infrastructure","Demo Vendor",amount("100"),Currency.USD,today.minusDays(1),false,null,"Development seed",null));
            }
            for(int i=0;i<5;i++) leads.create(new LeadDtos.Request("Demo · Opportunity "+(i+1),"Synthetic prospect",null,null,"prospect"+i+"@example.test","Referral","Web application",amount("1500"),Currency.USD,
                List.of("NEW","CONTACTED","QUALIFIED","PROPOSAL_SENT","NEGOTIATION").get(i),"NORMAL",clock.instant().plusSeconds(i*86400L),UUID.fromString(owner),"Development seed",null,null));
        } finally { SecurityContextHolder.clearContext(); }
    }
}
