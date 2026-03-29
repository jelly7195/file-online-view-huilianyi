package cn.keking.web.filter;

import cn.keking.config.ConfigConstants;
import cn.keking.utils.WebUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

/**
 * @author chenjh
 * @since 2020/2/18 19:13
 */
public class TrustHostFilter implements Filter {

    private String notTrustHostHtmlView;

    @Override
    public void init(FilterConfig filterConfig) {
        ClassPathResource classPathResource = new ClassPathResource("web/notTrustHost.html");
        try {
            classPathResource.getInputStream();
            byte[] bytes = FileCopyUtils.copyToByteArray(classPathResource.getInputStream());
            this.notTrustHostHtmlView = new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        String url = WebUtils.getSourceUrl(request);
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String host = WebUtils.getHost(url);
        assert host != null;

        if(!isValidUrl(url)){
            request.setAttribute("errorMsg", "文件地址不合法:"+ url);
            // 转发到控制器
            request.getRequestDispatcher("/handleError").forward(req, res);
            return;
        }
        if (isNotTrustHost(host)) {
            // 对 host 进行 HTML 实体编码,防止xss攻击
//            String escapedHost = StringEscapeUtils.escapeHtml4(host);//不可信域名
//            String html = this.notTrustHostHtmlView.replace("${current_host}", escapedHost);
            request.setAttribute("errorMsg", "预览源文件来自不受信任的站点，"+ host);
            // 转发到控制器
            request.getRequestDispatcher("/handleError").forward(req, res);
//            response.getWriter().write(html);
//            response.getWriter().close();
        } else {
            chain.doFilter(request, response);
        }
    }

    /**
     * 严格检查 URL 是否合法
     * @param urlStr 待检查的 URL 字符串
     * @return 如果 URL 合法返回 true，否则返回 false
     */
    public static boolean isValidUrl(String urlStr) {
        if (urlStr == null || urlStr.isEmpty()) {
            return false;
        }
        if (urlStr.contains("<") || urlStr.contains(">") || urlStr.contains("\"")) {
            return false;
        }
        return true;
    }

    public boolean isNotTrustHost(String host) {
        //黑名单
        if (CollectionUtils.isNotEmpty(ConfigConstants.getNotTrustHostSet())) {
            return ConfigConstants.getNotTrustHostSet().contains(host);
        }

        //白名单
        if (ConfigConstants.getTrustHosts().length > 0) {//白名单
            return !WebUtils.isTrustHost(ConfigConstants.getTrustHosts(),host);
        }
        return false;
    }

    @Override
    public void destroy() {

    }

}
