package jree.client_server.server;

public class LoginSignupException extends Exception {

    public final static LoginSignupException _409 = new LoginSignupException(409);
    public final static LoginSignupException _500 = new LoginSignupException(500);
    public final static LoginSignupException _401 = new LoginSignupException(401);

    private final int code;

    public LoginSignupException(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
