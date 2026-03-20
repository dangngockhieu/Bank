package vn.bank.khieu;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class KhieuApplication {

	public static void main(String[] args) {
		EnvLoader.loadEnv();
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
		SpringApplication.run(KhieuApplication.class, args);
	}

}
