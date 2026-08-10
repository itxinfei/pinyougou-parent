package entity;

import java.io.Serializable;

public class Result implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean success;
    private String message;
    private Object data;

    public Result(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public Result(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static Result buildResult(boolean success, String message) {
        return new Result(success, message);
    }

    public static Result buildResult(boolean success, String message, Object data) {
        return new Result(success, message, data);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

}
