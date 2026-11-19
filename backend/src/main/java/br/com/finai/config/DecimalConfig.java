package br.com.finai.config;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.context.annotation.*;
import java.math.BigDecimal;
@Configuration
public class DecimalConfig {
    @Bean SimpleModule decimaisExatos() {
        SimpleModule module=new SimpleModule(); module.addSerializer(BigDecimal.class,ToStringSerializer.instance); return module;
    }
}
