package com.github.biltudas1.swiftserve;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

final record Response(Boolean status, String message) {
}

@SpringBootApplication
@RestController
public class SwiftserveApplication {
	public static void main(String[] args) {
		SpringApplication.run(SwiftserveApplication.class, args);
	}

	@GetMapping("/")
	public String root() {
		Block blk = new Block(0, "abcd", "add_node", "data-some", "127.0.0.1", "no-signature");
		return blk.getHash();
	}

	@GetMapping("/download")
	public Response hello(@RequestParam(value = "file", defaultValue = "") String name) {
		if (name.isBlank()) {
			return new Response(false, "Please provide a filename");
		}
		return new Response(true, String.format("Downloading %s!", name));
	}
}
