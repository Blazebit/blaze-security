package com.blazebit.security.auth;

import org.jboss.security.auth.spi.InputValidationException;
import org.jboss.security.auth.spi.InputValidator;

public class ShowcaseInputValidator implements InputValidator {

    @Override
    public void validateUsernameAndPassword(String username, String password) throws InputValidationException {
        // TODO
        // throw new InputValidationException();
    }

}
