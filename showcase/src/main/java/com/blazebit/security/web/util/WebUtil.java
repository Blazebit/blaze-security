package com.blazebit.security.web.util;

import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * @author Christian Beikov
 * 
 * @company Curecomp Gmbh
 * @date 12.01.2012
 */
public final class WebUtil {

    /**
     * Answers the question if current request is a tabChange request
     * 
     * @return true if this request is a tabChange request
     */
    public static boolean isTabChangeRequest() {
        return "tabChange".equals(FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("javax.faces.partial.event"));
    }

    /**
     * Answers the question if current request is a filter request
     * 
     * @return true if this request is a filter request
     */
    public static boolean isFilterRequest() {
        return "filter".equals(FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("javax.faces.partial.event"));
    }

    /**
     * Answers the question if current request is a sorting request
     * 
     * @return true if this request is a sorting request
     */
    public static boolean isSortingRequest() {
        return "sort".equals(FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("javax.faces.partial.event"));
    }

    /**
     * Answers the question if current request is a selection request
     * 
     * @return true if this request is a selection request
     */
    public static boolean isRowSelectRequest() {
        return "rowSelect".equals(FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("javax.faces.partial.event"));
    }

    /**
     * Answers the question if current request is a change request
     * 
     * @return true if the current request is a change request
     */
    public static final boolean isChangeRequest() {
        return "change".equals(FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("javax.faces.partial.event"));
    }

    /**
     * Answers the question if data is reloadable or not. If it is a sorting or filter request it will be considered as not
     * reloadable.
     * 
     * @return true if the data is reloadable
     */
    public static boolean isReloadable() {
        return (!WebUtil.isFilterRequest() && !WebUtil.isSortingRequest());
    }

    /**
     * Redirects to the given URI via the given {@link FacesContext}.
     * 
     * @param fc the current FacesContext
     * @param uri the URI to be redirected to.
     * @param invalidate true if the session shall be invalidated
     * @throws InternalErrorException if the redirect fails
     */
    public static void redirect(final FacesContext fc, final String uri, boolean invalidate) {
        try {
            if (invalidate) {
                ((HttpSession) fc.getExternalContext().getSession(false)).invalidate();
            }
            fc.getExternalContext().redirect(uri);
            fc.setViewRoot(new UIViewRoot());
        } catch (Exception e) {
            throw new InternalError();
        }
    }

    /**
     * Redirects the current request to the given uri via the {@link RequestDispatcher} of the given {@link HttpServletRequest}
     * if the current request is no ajax request. If it is an ajax request the redirect will be done via the JSF ajax response
     * on the client side.
     * 
     * @param req the {@link HttpServletRequest}
     * @param res the {@link HttpServletResponse}
     * @param uri the uri o be redirected
     * @param invalidate true if the session shall be invalidated
     */
    public static void redirect(final HttpServletRequest request, final HttpServletResponse response, String uri, boolean invalidate) {
        if (invalidate) {
            request.getSession().invalidate();
        }
        try {
            response.setDateHeader("Expires", 0);
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setContentType("text/xml");
            response.setCharacterEncoding("UTF-8");

            if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
                response
                    .getWriter()
                    .append("<?xml version=\"1.0\" encoding=\"utf-8\"?><partial-response><changes><redirect url=\"")
                    .append(uri)
                    .append("\"/></changes></partial-response>");
                response.getWriter().flush();
            } else {
                // Seems no ContextRoot on redirect is necessary
                uri = uri.replace("Clevercure/", "");
                request.getRequestDispatcher(uri).forward(request, response);
            }
        } catch (Exception e) {
            throw new InternalError("Could not redirect via RequestDispatcher !!! uri: " + uri);
        }
    }
}
