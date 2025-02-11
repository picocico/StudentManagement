package raisetech.student.management;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class Application {

	private String name = "Pico Cico";
	private String age = "31";

	// Mapを作成
	private Map<String, String> students = new HashMap<>();

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	// GETは取得する、リクエストの結果を受け取る
	@GetMapping("/studentInfo")
	public String getStudentInfo() {
		return name + " " + age + "歳";
	}

	// MapをGETする
	@GetMapping("/studentMap")
	public Map <String, String> getStudentMap() {
		return students;
	}

	// POSTは情報を与える、渡す
	@PostMapping("/studentInfo")
	public void setStudentInfo(String name, String age) {
		this.name = name;
	  this.age = age;
	}
  // nameだけPOSTする
	@PostMapping("/studentName")
	public void updateStudentName(String name) {
		this.name = name;
	}

	// MapにPOSTする
	@PostMapping("/studentMap")
	public void addStudentMap(String studentNumber, String studentName) {
		students.put(studentNumber,studentName);
	}

	// MapからDeleteする
	@DeleteMapping("/studentMap")
	public void deleteStudentMap(@RequestParam String studentNumber) {
		students.remove(studentNumber);

	}
}

