package fi.fmi.avi.converter.iwxxm.conf;

import fi.fmi.avi.converter.iwxxm.profile.Aixm511FullSchemaProfile;
import fi.fmi.avi.converter.iwxxm.profile.Aixm511WxSchemaProfile;
import fi.fmi.avi.converter.iwxxm.profile.IWXXMSchemaProfile;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * IWXXM 2021-2 and later depend on AIXM 5.1.1 full schema.
 * IWXXM 3.0 and older depend on AIXM 5.1.1 WX schema.
 */
@Configuration
public class IWXXMSchemaProfilesConfig {

    @Bean
    public IWXXMSchemaProfile aixmWxSchemaProfile() {
        return new Aixm511WxSchemaProfile();
    }

    @Bean
    public IWXXMSchemaProfile aixmFullSchemaProfile() {
        return new Aixm511FullSchemaProfile();
    }

}

