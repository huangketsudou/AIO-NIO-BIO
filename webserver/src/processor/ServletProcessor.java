package processor;

import connector.*;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class ServletProcessor {


    URLClassLoader getServletLoader() throws MalformedURLException {
        File webRoot = new File(ConnectorUtils.WEB_ROOT);
        URL webRootUrl = webRoot.toURI().toURL();
        return new URLClassLoader(new URL[]{webRootUrl});
    }

    Servlet getServlet(URLClassLoader loader, Request request) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        // servlet/TimeServlet
        String uri = request.getRequestUri();
        String servletName = uri.substring(uri.lastIndexOf("/") + 1);
        Class servletClass = loader.loadClass(servletName);
        Servlet servlet = (Servlet) servletClass.newInstance();
        return servlet;
    }

    public void process(Request request, Response response) throws IOException {
        URLClassLoader loader = getServletLoader();
        try {
            Servlet servlet = getServlet(loader, request);
            RequestFacade requestFacade = new RequestFacade(request);
            ResponseFacade responseFacade = new ResponseFacade(response);
            servlet.service(requestFacade, responseFacade);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (ServletException e) {
            e.printStackTrace();
        }
    }
}