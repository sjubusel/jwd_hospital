package by.epamtc.jwd.auth.web.util.impl.subprofile;

import by.epamtc.jwd.auth.model.auth_info.AuthUser;
import by.epamtc.jwd.auth.model.constant.AppAttribute;
import by.epamtc.jwd.auth.model.constant.AppParameter;
import by.epamtc.jwd.auth.model.constant.ChangeResult;
import by.epamtc.jwd.auth.service.AuthUserService;
import by.epamtc.jwd.auth.service.ServiceFactory;
import by.epamtc.jwd.auth.service.exception.ServiceException;
import by.epamtc.jwd.auth.web.util.Command;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ProfileChangePasswordCommand implements Command {
    private ServiceFactory serviceFactory = ServiceFactory.getInstance();
    private AuthUserService authUserService = serviceFactory.getAuthUserService();

    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        AuthUser user = (AuthUser) req.getSession().getAttribute(AppAttribute
                .SESSION_AUTH_USER);
        String password = req.getParameter(AppParameter.PASSWORD);
        String newPassword = req.getParameter(AppParameter.NEW_PASSWORD);

        String result = null;

        try {
            result = authUserService.changePassword(newPassword, password, user);
        } catch (ServiceException e) {
            // TODO loj4j
//            sendRedirectWithTechError(req, res);
        }

        if (result == null) {
//            sendRedirectWithValidationError(req, res);
            return;
        }

        if (result.equals(ChangeResult.DUPLICATE_ERROR.name())) {
//            sendRedirectWithDuplicateError(req, res);
            return;
        }

        if (result.equals(ChangeResult.ILLEGAL_PASSWORD.name())) {
//            sendRedirectWithIllegalPasswordError(req, res);
            return;
        }

//        sendRedirectWithSuccessMessage(req, res);


    }
}
