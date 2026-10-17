package com.codevo.businessos.search;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import com.codevo.businessos.common.*;
@Service
public class SearchService {
    public record Result(UUID id,String type,String title,String route,String subtitle,java.math.BigDecimal amount,String currency) {}
    private final JdbcTemplate jdbc;
    public SearchService(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public List<Result> search(String text,boolean finance,boolean sales) {
        if(text==null || text.trim().length()<2) return List.of();
        if(text.length()>120) throw DomainException.invalid("Search is too long");
        var queries=new ArrayList<String>();
        queries.add(select("clients","display_name","/app/clients/","clients"));
        queries.add(select("projects","name","/app/projects/","projects"));
        queries.add(select("tasks","title","/app/tasks?focus=","tasks"));
        if(sales) queries.add(select("leads","name","/app/leads?focus=","leads"));
        if(finance) {
            queries.add("(select i.id,'invoices' type,i.invoice_number title,concat('/app/finance/invoices/',i.id) route,c.display_name subtitle,i.balance_due amount,i.currency from invoices i join clients c on c.id=i.client_id where i.invoice_number like ? escape '=' order by i.invoice_number limit 10)");
            queries.add(select("subscriptions","name","/app/finance/subscriptions?focus=","subscriptions"));
            queries.add("(select ch.id,'checks' type,ch.check_number title,concat('/app/finance/checks?focus=',ch.id) route,ch.bank_name subtitle,ch.amount,ch.currency from check_payments ch where ch.check_number like ? escape '=' order by ch.check_number limit 10)");
        }
        String like=ListFilter.escape(text.trim())+"%";
        return jdbc.query("select * from ("+String.join(" union all ",queries)+") results order by title limit 30",
            (rs,n)->new Result(com.codevo.businessos.common.SqlIds.uuid(rs,1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getBigDecimal(6),rs.getString(7)),
            Collections.nCopies(queries.size(),like).toArray());
    }
    private String select(String table,String field,String route,String type) {
        return "(select id,'"+type+"' type,"+field+" title,concat('"+route+"',id) route,null subtitle,cast(null as decimal(19,3)) amount,null currency from "+table+" where "+field+" like ? escape '=' order by "+field+" limit 10)";
    }
}
