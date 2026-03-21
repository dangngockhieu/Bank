package vn.bank.khieu.utils.format_response;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RestResponse<T> {
    private int statusCode;
    private String error;
    private Object message;
    private T data;
}