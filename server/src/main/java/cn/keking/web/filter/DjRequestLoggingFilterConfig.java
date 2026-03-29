package cn.keking.web.filter;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;


/**
 * .
 *
 * @author leven.chen
 */
@Profile("dj")
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 90)
public class DjRequestLoggingFilterConfig implements Filter {

    private static final Logger log = LoggerFactory.getLogger(DjRequestLoggingFilterConfig.class);

    private static final String UNKNOWN = "";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Do nothing because of X and Y.

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = null;
        if (servletRequest instanceof HttpServletRequest) {
            httpServletRequest = (HttpServletRequest) servletRequest;
        }

        HttpServletResponse httpServletResponse = null;
        if (servletResponse instanceof HttpServletResponse) {
            httpServletResponse = (HttpServletResponse) servletResponse;
        }


        setBeforeAccessLoggingInfo(servletRequest, servletResponse, httpServletRequest, httpServletResponse);
        filterChain.doFilter(servletRequest, servletResponse);
        setAfterAccessLoggingInfo(servletRequest, servletResponse, httpServletRequest, httpServletResponse);
    }


    @Override
    public void destroy() {
        // Do nothing because of X and Y.
    }

    /**
     * set access info  to MDC.
     *
     * @param servletRequest      servletRequest
     * @param servletResponse     servletResponse
     * @param httpServletRequest  httpServletRequest
     * @param httpServletResponse httpServletResponse
     */
    private void setBeforeAccessLoggingInfo(ServletRequest servletRequest, ServletResponse servletResponse, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        String clientIp = getClientIp(httpServletRequest, httpServletResponse, servletRequest, servletResponse);
        MDC.put(DjiLoggingFieldConstant.CLIENT_IP,clientIp);

        String requestId = getRequestId(httpServletRequest, httpServletResponse, servletRequest, servletResponse);
        MDC.put(DjiLoggingFieldConstant.REQUEST_ID,requestId);

        String userAgent = getUserAgent(httpServletRequest, httpServletResponse, servletRequest, servletResponse);
        MDC.put(DjiLoggingFieldConstant.USER_AGENT,userAgent);

        String method = getMethod(httpServletRequest, httpServletResponse, servletRequest, servletResponse);
        MDC.put(DjiLoggingFieldConstant.METHOD,method);

        String referer = getReferer(httpServletRequest, httpServletResponse, servletRequest, servletResponse);
        MDC.put(DjiLoggingFieldConstant.REFERER,referer);

        String path = getPath(httpServletRequest, httpServletResponse, servletRequest, servletResponse);
        MDC.put(DjiLoggingFieldConstant.PATH,path);

        long start = System.currentTimeMillis(); // 请求进入时间
        servletRequest.setAttribute(DjiLoggingFieldConstant.REQUEST_BEGIN_TIME_MILLIS,start);

        String cloudbusNonce = getCloudbusNonce(httpServletRequest, httpServletResponse, servletRequest, servletResponse);
        MDC.put(DjiLoggingFieldConstant.CLOUDBUS_NONCE,cloudbusNonce);

        // String user = getUser(httpServletRequest, httpServletResponse, servletRequest, servletResponse);
        // MDC.put(DjiLoggingFieldConstant.USER, user);

    }

    private void setAfterAccessLoggingInfo(ServletRequest servletRequest, ServletResponse servletResponse,
                                           HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        String status = getStatus(httpServletRequest, httpServletResponse, servletRequest, servletResponse);
        MDC.put(DjiLoggingFieldConstant.STATUS,status);

        String duration = getDuration(httpServletRequest, httpServletResponse, servletRequest, servletResponse);
        MDC.put(DjiLoggingFieldConstant.DURATION,duration);
    }

    private String getDuration(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
                               ServletRequest servletRequest, ServletResponse servletResponse) {

        long end = System.currentTimeMillis(); // 请求进入时间
        Long start = (Long) servletRequest.getAttribute(DjiLoggingFieldConstant.REQUEST_BEGIN_TIME_MILLIS);
        if (Objects.nonNull(start)) {
            return (end - start) + "ms";
        }
        return UNKNOWN;
    }

    private String getStatus(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
                             ServletRequest servletRequest, ServletResponse servletResponse) {
        if (Objects.nonNull(httpServletResponse)) {
            return httpServletResponse.getStatus() + "";
        }
        return UNKNOWN;
    }

    private String getPath(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
                           ServletRequest servletRequest, ServletResponse servletResponse) {
        if (Objects.nonNull(httpServletRequest)) {
            return httpServletRequest.getRequestURI();
        }
        return UNKNOWN;
    }

    private String getReferer(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
                              ServletRequest servletRequest, ServletResponse servletResponse) {
        if (Objects.nonNull(httpServletResponse)) {
            return httpServletResponse.getHeader(HttpHeaders.REFERER);
        }
        return UNKNOWN;
    }

    private String getMethod(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
                             ServletRequest servletRequest, ServletResponse servletResponse) {
        if (Objects.nonNull(httpServletRequest)) {
            return httpServletRequest.getMethod();
        }
        return UNKNOWN;
    }

    private String getUserAgent(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
                                ServletRequest servletRequest, ServletResponse servletResponse) {
        if (Objects.nonNull(httpServletResponse)) {
            return httpServletResponse.getHeader(HttpHeaders.USER_AGENT);
        }
        return UNKNOWN;
    }

    private String getRequestId(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
                                ServletRequest servletRequest, ServletResponse servletResponse) {
        String requestId = UUID.randomUUID().toString();
        requestId = requestId.replace("-", "");
        return requestId;
    }


    private String getClientIp(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
                               ServletRequest servletRequest, ServletResponse servletResponse) {
        if (Objects.nonNull(httpServletResponse)) {
            String ip = "";
            try {
                String ignoreText = "unknown";
                ip = httpServletRequest.getHeader("x-forwarded-for");
                if (ip == null || ip.length() == 0 || ip.equalsIgnoreCase(ignoreText)) {
                    ip = httpServletRequest.getHeader("Proxy-Client-IP");
                }
                if (ip == null || ip.length() == 0 || ip.equalsIgnoreCase(ignoreText)) {
                    ip = httpServletRequest.getHeader("WL-Proxy-Client-IP");
                }
                if (ip == null || ip.length() == 0 || ip.equalsIgnoreCase(ignoreText)) {
                    ip = httpServletRequest.getRemoteAddr();
                }
                return ip;
            } catch (Exception e) {
                log.warn("MDC try get client ip error", e);
            }
        }

        return UNKNOWN;
    }


    private String getCloudbusNonce(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
                                    ServletRequest servletRequest, ServletResponse servletResponse) {
        if (Objects.nonNull(httpServletResponse)) {
            return httpServletResponse.getHeader("Nonce-Gw-S");
        }
        return UNKNOWN;

    }

    // /**
    //  * 获取用户
    //  *
    //  * @return
    //  */
    // private String getUser(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
    //                        ServletRequest servletRequest, ServletResponse servletResponse) {
    //     try {
    //         String user;
    //         CustomUserDetails userDetail = DetailsHelper.getUserDetails();
    //         if (userDetail != null) {
    //             user = String.valueOf(userDetail.getUserId());
    //         } else {
    //             user = "";
    //         }
    //         return user;
    //     } catch (Exception ex) {
    //         log.warn("MDC try get user error ", ex);
    //     }
    //     return UNKNOWN;
    // }


    static final class DjiLoggingFieldConstant {

        public static final String CLIENT_IP = "client_ip";
        public static final String REQUEST_ID = "request_id";
        public static final String USER_AGENT = "user_agent";
        public static final String METHOD = "method";
        public static final String REFERER = "referer";
        public static final String PATH = "path";
        public static final String CLOUDBUS_NONCE = "cloudbus_nonce";

        public static final String USER = "user";
        /**
         * 请求返回状态
         */
        public static final String STATUS = "status";
        /**
         * 响应时间
         */
        public static final String DURATION = "duration";

        /**
         * 请求开始时间
         */
        public static final String REQUEST_BEGIN_TIME_MILLIS = "request_begin_time_millis";
        /**
         * 请求相应时间
         */
        public static final String REQUEST_END_TIME_MILLIS = "request_end_time_millis";
        /**
         * 链路 id，可用于全链路日志追踪
         */
        public static final String TRACE_ID = "trace_id";
        /**
         * 服务器 ip
         */
        public static final String SERVER_IP = "server_ip";
    }

}