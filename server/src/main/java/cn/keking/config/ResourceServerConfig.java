// package cn.keking.config;
//
//
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.context.annotation.Primary;
// import org.springframework.core.env.Environment;
// import org.springframework.security.config.annotation.web.builders.HttpSecurity;
// import org.springframework.security.oauth2.client.OAuth2ClientContext;
// import org.springframework.security.oauth2.client.token.AccessTokenRequest;
// import org.springframework.security.oauth2.client.token.DefaultAccessTokenRequest;
// import org.springframework.security.oauth2.common.OAuth2AccessToken;
// import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
// import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
// import org.springframework.security.web.firewall.HttpFirewall;
// import org.springframework.security.web.firewall.StrictHttpFirewall;
//
// import javax.annotation.Resource;
// import java.io.Serializable;
// import java.util.Arrays;
// import java.util.HashMap;
// import java.util.Map;
//
// @Configuration
// @EnableResourceServer
// public class ResourceServerConfig extends ResourceServerConfigurerAdapter {
//
//    @Resource
//    private Environment environment;
//
//     @Bean
//     public HttpFirewall allowUrlEncodedSlashHttpFirewall() {
//         StrictHttpFirewall firewall = new StrictHttpFirewall();
//         firewall.setAllowSemicolon(true);
//         return firewall;
//     }
//
//     @Override
//     public void configure(HttpSecurity http) throws Exception {
//         http
//                 .authorizeRequests()
//                 .antMatchers(Arrays.asList(environment.getActiveProfiles()).contains("local")?"/**":"/xx").permitAll()
//                 .antMatchers("/onlinePreview","/picturesPreview").authenticated()
//                 .and().csrf().disable().headers().frameOptions().disable()
//                 .and().cors().disable();
//     }
//
//     @Primary
//     @Bean("customizedOAuth2ClientContext")
//     public OAuth2ClientContext customizedOAuth2ClientContext() {
//         return new CustomizedOAuth2ClientContext();
//     }
//
//     static class CustomizedOAuth2ClientContext implements OAuth2ClientContext, Serializable {
//
//         // make accessToken thread local to avoid thread safe issue
//         private final ThreadLocal<OAuth2AccessToken> accessToken = new ThreadLocal<>();
//
//         private final AccessTokenRequest accessTokenRequest;
//
//         private final Map<String, Object> state = new HashMap<>();
//
//         public CustomizedOAuth2ClientContext() {
//             this(new DefaultAccessTokenRequest());
//         }
//
//         public CustomizedOAuth2ClientContext(AccessTokenRequest accessTokenRequest) {
//             this.accessTokenRequest = accessTokenRequest;
//         }
//
//         @Override
//         public OAuth2AccessToken getAccessToken() {
//             return accessToken.get();
//         }
//
//         @Override
//         public void setAccessToken(OAuth2AccessToken accessToken) {
//             this.accessToken.set(accessToken);
//             this.accessTokenRequest.setExistingToken(accessToken);
//         }
//
//         @Override
//         public AccessTokenRequest getAccessTokenRequest() {
//             return accessTokenRequest;
//         }
//
//         @Override
//         public void setPreservedState(String stateKey, Object preservedState) {
//             state.put(stateKey, preservedState);
//         }
//
//         @Override
//         public Object removePreservedState(String stateKey) {
//             return state.remove(stateKey);
//         }
//
//     }
// }
